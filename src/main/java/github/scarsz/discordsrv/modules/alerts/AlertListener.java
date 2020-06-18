package github.scarsz.discordsrv.modules.alerts;

import alexh.weak.Dynamic;
import alexh.weak.Weak;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.RegisteredListener;

import java.lang.reflect.Field;
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
            onCommand(
                    ((PlayerCommandPreprocessEvent) event).getPlayer(),
                    ((PlayerCommandPreprocessEvent) event).getMessage()
            );
            return;
        } else if (event instanceof ServerCommandEvent) {
            onCommand(
                    ((ServerCommandEvent) event).getSender(),
                    ((ServerCommandEvent) event).getCommand()
            );
            return;
        }

        List<Dynamic> alerts = DiscordSRV.config().dget("Alerts").children().collect(Collectors.toList());
        for (int i = 0; i < alerts.size(); i++) {
            Dynamic alert = alerts.get(i);
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

            MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("Alerts." + i);
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
        }
    }

    private void onCommand(CommandSender sender, String command) {
        //TODO
    }

}
