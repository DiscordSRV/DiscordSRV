package github.scarsz.discordsrv.modules.alerts;

import alexh.weak.Dynamic;
import alexh.weak.Weak;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.Event;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.plugin.RegisteredListener;

import java.awt.*;
import java.lang.reflect.Field;
import java.util.List;
import java.util.*;
import java.util.stream.Collectors;

public class AlertListener implements Listener {

    static {
        List<HandlerList> globalHandlerListLocal = null;
        try {
            Field field = HandlerList.class.getDeclaredField("allLists");
            field.setAccessible(true);
            globalHandlerListLocal = new ArrayList<HandlerList>((List<HandlerList>) field.get(null)) {
                @Override
                public boolean add(HandlerList list) {
                    boolean added = super.add(list);
                    if (Arrays.stream(list.getRegisteredListeners()).noneMatch(listener::equals)) list.register(listener);
                    return added;
                }
            };
            field.set(null, globalHandlerListLocal);
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        HANDLERS = globalHandlerListLocal;
    }

    private static final List<HandlerList> HANDLERS;
    private static final Set<String> SEEN_EVENTS = new HashSet<>();
    private static RegisteredListener listener;

    public void register() {
        listener = new RegisteredListener(
                new Listener() {},
                (listener, event) -> onEvent(event),
                EventPriority.MONITOR,
                DiscordSRV.getPlugin(),
                false
        );

        Set<String> events = DiscordSRV.config().dget("Alerts").children()
                .map(dynamic -> dynamic.get("Trigger"))
                .filter(Weak::isPresent)
                .map(dynamic -> dynamic.convert().intoString())
                .filter(s -> !s.startsWith("/"))
                .collect(Collectors.toSet());

        DiscordSRV.debug("Registered alerts for " + String.join(", ", events));

        long count = DiscordSRV.config().dget("Alerts").children().count();
        if (count > 0) DiscordSRV.info(count + " alert" + (count > 1 ? "s" : "") + " registered");
    }

    public void unregister() {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            handlerList.unregister(listener);
        }
    }

    public <E extends Event> void onEvent(E event) {
        if (event instanceof PlayerCommandPreprocessEvent) {
            onCommand((PlayerCommandPreprocessEvent) event);
            return;
        }

        DiscordSRV.config().dget("Alerts").children().forEach(alert -> {
            String triggerEvent = alert.get("Trigger").convert().intoString();

            // make sure the called event matches what this alert is supposed to trigger on
            if (!event.getEventName().equalsIgnoreCase(triggerEvent)) return;

            Player player = event instanceof PlayerEvent ? ((PlayerEvent) event).getPlayer() : null;

            // make sure channel is available
            String gameChannel = alert.get("Channel").asString();
            if (gameChannel == null) {
                DiscordSRV.debug("Not running alert for event " + triggerEvent + ": no target channel was defined");
                return;
            }
            TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(gameChannel);
            if (channel == null) {
                DiscordSRV.debug("Not running alert for event " + triggerEvent + ": target TextChannel was not available");
                return;
            }

            // make sure alert should run even if event is cancelled
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                Boolean ignoreCancelled = alert.get("IgnoreCancelled").as(Boolean.class);
                if (ignoreCancelled == null || ignoreCancelled) {
                    DiscordSRV.debug("Not running alert for event " + triggerEvent + ": event was cancelled");
                    return;
                }
            }

            // check alert conditions
            boolean anyConditionFailed = alert.dget("Conditions").children()
                    .anyMatch(dynamic -> {
                        String expression = dynamic.convert().intoString();
                        Boolean value = new SpELExpressionBuilder(expression)
                                .withPluginVariables()
                                .withVariable("event", event)
                                .withVariable("server", Bukkit.getServer())
                                .withVariable("discordsrv", DiscordSRV.getPlugin())
                                .withVariable("player", player)
                                .withVariable("channel", channel)
                                .withVariable("jda", DiscordUtil.getJda())
                                .evaluate(event, Boolean.class);
                        DiscordSRV.debug("Condition \"" + expression + "\" -> " + value);
                        return !(value != null ? value : false);
                    });
            if (anyConditionFailed) return;

            // construct the alert message
            MessageFormat messageFormat = new MessageFormat();

            Dynamic contentDynamic = alert.dget("Content");
            if (contentDynamic.isPresent() && StringUtils.isNotBlank(contentDynamic.asString())) messageFormat.setContent(contentDynamic.asString());

            if (alert.dget("Webhook").isPresent()) {
                boolean enabled = alert.dget("Webhook.Enable").isPresent() ? alert.dget("Webhook.Enable").as(boolean.class) : true;
                if (enabled) {
                    messageFormat.setUseWebhooks(true);

                    Dynamic avatarUrlDynamic = alert.dget("Webhook.AvatarUrl");
                    if (avatarUrlDynamic.isPresent() && StringUtils.isNotBlank(avatarUrlDynamic.asString())) messageFormat.setWebhookAvatarUrl(avatarUrlDynamic.asString());

                    Dynamic nameDynamic = alert.dget("Webhook.Name");
                    if (nameDynamic.isPresent() && StringUtils.isNotBlank(nameDynamic.asString())) messageFormat.setWebhookName(nameDynamic.asString());
                }
            }

            if (alert.get("Embed").isPresent()) {
                boolean enabled = alert.dget("Embed.Enabled").isPresent() ? alert.dget("Embed.Enabled").as(Boolean.class) : true;
                if (enabled) {
                    Dynamic colorDynamic = alert.dget("Embed.Color");
                    if (colorDynamic.isPresent()) {
                        String hex = colorDynamic.asString().trim().replace("#", "");
                        if (hex.length() == 6) {
                            messageFormat.setColor(
                                    new Color(
                                            Integer.valueOf(hex.substring(0, 2), 16),
                                            Integer.valueOf(hex.substring(2, 4), 16),
                                            Integer.valueOf(hex.substring(4, 6), 16)
                                    )
                            );
                        }
                    }

                    Dynamic authorDynamic = alert.dget("Embed.Author");
                    if (authorDynamic.isPresent()) {
                        Dynamic authorNameDynamic = authorDynamic.get("Name");
                        if (authorNameDynamic.isPresent() && StringUtils.isNotBlank(authorNameDynamic.convert().intoString())) messageFormat.setAuthorName(authorNameDynamic.convert().intoString());
                        Dynamic authorUrlDynamic = authorDynamic.get("Url");
                        if (authorUrlDynamic.isPresent() && StringUtils.isNotBlank(authorUrlDynamic.convert().intoString())) messageFormat.setAuthorUrl(authorUrlDynamic.convert().intoString());
                        Dynamic authorImageUrl = authorDynamic.get("ImageUrl");
                        if (authorImageUrl.isPresent() && StringUtils.isNotBlank(authorImageUrl.convert().intoString())) messageFormat.setAuthorImageUrl(authorImageUrl.convert().intoString());
                    }

                    Dynamic thumbnailUrlDynamic = alert.dget("Embed.ThumbnailUrl");
                    if (thumbnailUrlDynamic.isPresent() && StringUtils.isNotBlank(thumbnailUrlDynamic.convert().intoString())) messageFormat.setThumbnailUrl(thumbnailUrlDynamic.convert().intoString());

                    Dynamic titleDynamic = alert.dget("Embed.Title");
                    if (titleDynamic.isPresent()) {
                        Dynamic textDynamic = titleDynamic.get("Text");
                        if (textDynamic.isPresent() && StringUtils.isNotBlank(textDynamic.convert().intoString())) messageFormat.setTitle(textDynamic.convert().intoString());
                        Dynamic urlDynamic = titleDynamic.get("Url");
                        if (urlDynamic.isPresent() && StringUtils.isNotBlank(urlDynamic.convert().intoString())) messageFormat.setTitleUrl(urlDynamic.convert().intoString());
                    }

                    Dynamic descriptionDynamic = alert.dget("Embed.Description");
                    if (descriptionDynamic.isPresent() && StringUtils.isNotBlank(descriptionDynamic.convert().intoString())) messageFormat.setDescription(descriptionDynamic.convert().intoString());

                    Dynamic fieldsDynamic = alert.dget("Embed.Fields");
                    if (fieldsDynamic.isPresent()) {
                        List<MessageEmbed.Field> fields = new LinkedList<>();
                        fieldsDynamic.children().map(Object::toString).forEach(field -> {
                            if (field.contains(";")) {
                                String[] parts = field.split(";");
                                if (parts.length < 2) {
                                    return;
                                }

                                boolean inline = parts.length < 3 || Boolean.parseBoolean(parts[2]);
                                fields.add(new MessageEmbed.Field(parts[0], parts[1], inline, true));
                            } else {
                                boolean inline = Boolean.parseBoolean(field);
                                fields.add(new MessageEmbed.Field("\u200e", "\u200e", inline, true));
                            }
                        });
                        messageFormat.setFields(fields);
                    }

                    Dynamic imageUrlDynamic = alert.dget("Embed.ImageUrl");
                    if (imageUrlDynamic.isPresent() && StringUtils.isNotBlank(imageUrlDynamic.convert().intoString())) messageFormat.setImageUrl(imageUrlDynamic.convert().intoString());

                    Dynamic footerDynamic = alert.dget("Embed.Footer");
                    if (footerDynamic.isPresent()) {
                        Dynamic textDynamic = footerDynamic.get("Text");
                        if (textDynamic.isPresent() && StringUtils.isNotBlank(textDynamic.convert().intoString())) messageFormat.setFooterText(textDynamic.convert().intoString());
                        Dynamic iconUrlDynamic = footerDynamic.get("IconUrl");
                        if (iconUrlDynamic.isPresent() && StringUtils.isNotBlank(iconUrlDynamic.convert().intoString())) messageFormat.setFooterIconUrl(iconUrlDynamic.convert().intoString());
                    }

                    Dynamic timestampDynamic = alert.dget("Embed.Timestamp");
                    if (timestampDynamic.isPresent() && timestampDynamic.as(boolean.class)) messageFormat.setTimestamp(new Date().toInstant());
                }
            }

            Message message = DiscordSRV.getPlugin().translateMessage(messageFormat, (content, needsEscape) -> {
                if (content == null) return null;

                // evaluate any SpEL expressions
                content = NamedValueFormatter.formatExpressions(content, event,
                        "event", event,
                        "player", player,
                        "channel", channel
                );

                // replace any normal placeholders
                content = NamedValueFormatter.format(content, key -> {
                    switch (key) {
                        case "tps":
                            return Lag.getTPSString();
                        case "time":
                        case "date":
                            return TimeUtil.timeStamp();
                        case "ping":
                            return player != null ? PlayerUtil.getPing(player) : "-1";
                        case "name":
                        case "username":
                            return player != null ? player.getName() : "";
                        case "displayname":
                            return player != null ? DiscordUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(player.getDisplayName()) : player.getDisplayName()) : "";
                        case "world":
                            return player != null ? player.getWorld().getName() : "";
                        case "embedavatarurl":
                            return player != null ? DiscordSRV.getPlugin().getEmbedAvatarUrl(player) : DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
                        case "botavatarurl":
                            return DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
                        case "botname":
                            return DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
                        default:
                            return "{" + key + "}";
                    }
                });

                content = DiscordUtil.translateEmotes(content, channel.getGuild());
                content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
                return content;
            });

            channel.sendMessage(message).queue();
        });
    }

    private void onCommand(PlayerCommandPreprocessEvent event) {

    }

}
