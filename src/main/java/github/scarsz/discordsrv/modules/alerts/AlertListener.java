package github.scarsz.discordsrv.modules.alerts;

import alexh.weak.Dynamic;
import alexh.weak.Weak;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import org.apache.commons.lang3.StringUtils;
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

    private final RegisteredListener listener;

    private final List<Dynamic> alerts = new ArrayList<>();

    public AlertListener() {
        listener = new RegisteredListener(
                new Listener() {},
                (listener, event) -> onEvent(event),
                EventPriority.MONITOR,
                DiscordSRV.getPlugin(),
                false
        );
        reloadAlerts();

        //
        // Bukkit's API has no easy way to listen for all events
        // The best thing you can do is add a listener to all the HandlerList's but that only works
        // *after* an event has been initialized, which isn't guaranteed to happen before DiscordSRV's initialization
        //
        // Thus, we have to resort to making a proxy HandlerList.allLists list that adds our listener whenever a new
        // handler list is created by an event being initialized
        //
        try {
            Field field = HandlerList.class.getDeclaredField("allLists");
            field.setAccessible(true);
            List<HandlerList> theHandlerList = (List<HandlerList>) field.get(null);
            theHandlerList.forEach(this::addListener);
            field.set(null, new ArrayList<HandlerList>(theHandlerList) {
                @Override
                public boolean add(HandlerList list) {
                    boolean added = super.add(list);
                    addListener(list);
                    return added;
                }
            });
        } catch (NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private void addListener(HandlerList handlerList) {
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            if (stackTraceElement.getClassName().equals("com.destroystokyo.paper.event.player.PlayerHandshakeEvent")
                    && stackTraceElement.getMethodName().equals("<clinit>")) {
                // Don't register PlayerHandshakeEvent since Paper then assumes we're handling logins
                DiscordSRV.debug("Skipping registering HandlerList for Paper's PlayerHandshakeEvent for alerts");
                return;
            }
        }
        if (Arrays.stream(handlerList.getRegisteredListeners()).noneMatch(listener::equals)) handlerList.register(listener);
    }

    public void reloadAlerts() {
        alerts.clear();
        Optional<List<Map<?, ?>>> optionalAlerts = DiscordSRV.config().getOptional("Alerts");
        if (optionalAlerts.isPresent() && optionalAlerts.get().size() > 0) {
            long count = optionalAlerts.get().size();
            DiscordSRV.info(optionalAlerts.get().size() + " alert" + (count > 1 ? "s" : "") + " registered");

            for (Map<?, ?> map : optionalAlerts.get()) {
                alerts.add(Dynamic.from(map));
            }
        }
    }

    public void unregister() {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            handlerList.unregister(listener);
        }
    }

    private <E extends Event> void onEvent(E event) {
        Player player = event instanceof PlayerEvent ? ((PlayerEvent) event).getPlayer() : null;
        CommandSender sender = null;
        String command = null;
        List<String> args = new LinkedList<>();

        if (event instanceof PlayerCommandPreprocessEvent) {
            sender = player;
            command = ((PlayerCommandPreprocessEvent) event).getMessage().substring(1);
        } else if (event instanceof ServerCommandEvent) {
            sender = ((ServerCommandEvent) event).getSender();
            command = ((ServerCommandEvent) event).getCommand();
        }
        if (StringUtils.isNotBlank(command)) {
            String[] split = command.split(" ", 2);
            String commandBase = split[0];
            if (split.length == 2) args.addAll(Arrays.asList(split[1].split(" ")));

            // transform "discordsrv:discord" to just "discord" for example
            if (commandBase.contains(":")) commandBase = commandBase.substring(commandBase.lastIndexOf(":") + 1);

            command = commandBase + (split.length == 2 ? (" " + split[1]) : "");
        }

        for (int i = 0; i < alerts.size(); i++) {
            Dynamic alert = alerts.get(i);

            Set<String> triggers = new HashSet<>();
            Dynamic triggerDynamic = alert.get("Trigger");
            if (triggerDynamic.isList()) {
                triggers.addAll(triggerDynamic.children()
                        .map(Weak::asString)
                        .map(String::toLowerCase)
                        .collect(Collectors.toSet())
                );
            } else if (triggerDynamic.isString()) {
                triggers.add(triggerDynamic.asString().toLowerCase());
            }

            for (String trigger : triggers) {
                if (trigger.startsWith("/")) {
                    if (StringUtils.isBlank(command) || !command.toLowerCase().split("`\\s+|$`", 2)[0].equals(trigger.substring(1))) continue;
                } else {
                    // make sure the called event matches what this alert is supposed to trigger on
                    if (!event.getEventName().equalsIgnoreCase(trigger)) continue;
                }

                // make sure alert should run even if event is cancelled
                if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                    Dynamic ignoreCancelledDynamic = alert.get("IgnoreCancelled");
                    boolean ignoreCancelled = ignoreCancelledDynamic.isPresent() ? ignoreCancelledDynamic.as(boolean.class) : true;
                    if (ignoreCancelled) {
                        DiscordSRV.debug("Not running alert for event " + event.getEventName() + ": event was cancelled");
                        return;
                    }
                }

                Set<TextChannel> textChannels = new HashSet<>();
                Dynamic textChannelsDynamic = alert.get("Channel");
                if (textChannelsDynamic == null) {
                    DiscordSRV.debug("Not running alert for trigger " + trigger + ": no target channel was defined");
                    return;
                }
                if (textChannelsDynamic.isList()) {
                    textChannels.addAll(textChannelsDynamic.children()
                            .map(Weak::asString)
                            .map(s -> {
                                TextChannel target = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(s);
                                if (target == null) {
                                    DiscordSRV.debug("Not sending alert for trigger " + trigger + " to target channel "
                                            + s + ": TextChannel was not available");
                                }
                                return target;
                            })
                            .collect(Collectors.toSet())
                    );
                } else if (textChannelsDynamic.isString()) {
                    textChannels.add(DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(textChannelsDynamic.asString()));
                }
                textChannels.removeIf(Objects::isNull);
                if (textChannels.size() == 0) {
                    DiscordSRV.debug("Not running alert for trigger " + trigger + ": no target channel was defined");
                    return;
                }

                for (TextChannel textChannel : textChannels) {
                    // check alert conditions
                    boolean allConditionsMet = true;
                    Dynamic conditionsDynamic = alert.dget("Conditions");
                    if (conditionsDynamic.isPresent()) {
                        Iterator<Dynamic> conditions = conditionsDynamic.children().iterator();
                        while (conditions.hasNext()) {
                            Dynamic dynamic = conditions.next();
                            String expression = dynamic.convert().intoString();
                            Boolean value = new SpELExpressionBuilder(expression)
                                    .withPluginVariables()
                                    .withVariable("event", event)
                                    .withVariable("server", Bukkit.getServer())
                                    .withVariable("discordsrv", DiscordSRV.getPlugin())
                                    .withVariable("player", player)
                                    .withVariable("sender", sender)
                                    .withVariable("command", command)
                                    .withVariable("args", args)
                                    .withVariable("allArgs", String.join(" ", args))
                                    .withVariable("channel", textChannel)
                                    .withVariable("jda", DiscordUtil.getJda())
                                    .evaluate(event, Boolean.class);
                            DiscordSRV.debug("Condition \"" + expression + "\" -> " + value);
                            if (value != null && !value) {
                                allConditionsMet = false;
                                break;
                            }
                        }
                        if (!allConditionsMet) continue;
                    }

                    CommandSender finalSender = sender;
                    String finalCommand = command;
                    MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("Alerts." + i);
                    Message message = DiscordSRV.getPlugin().translateMessage(messageFormat, (content, needsEscape) -> {
                        if (content == null) return null;

                        // evaluate any SpEL expressions
                        Map<String, Object> variables = new HashMap<>();
                        variables.put("event", event);
                        variables.put("server", Bukkit.getServer());
                        variables.put("discordsrv", DiscordSRV.getPlugin());
                        variables.put("player", player);
                        variables.put("sender", finalSender);
                        variables.put("command", finalCommand);
                        variables.put("args", args);
                        variables.put("allArgs", String.join(" ", args));
                        variables.put("channel", textChannel);
                        variables.put("jda", DiscordUtil.getJda());
                        content = NamedValueFormatter.formatExpressions(content, event, variables);

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

                        content = DiscordUtil.translateEmotes(content, textChannel.getGuild());
                        content = PlaceholderUtil.replacePlaceholdersToDiscord(content, player);
                        return content;
                    });

                    if (messageFormat.isUseWebhooks()) {
                        WebhookUtil.deliverMessage(textChannel, messageFormat.getWebhookName(), messageFormat.getWebhookAvatarUrl(),
                                message.getContentRaw(), message.getEmbeds().stream().findFirst().orElse(null));
                    } else {
                        DiscordUtil.queueMessage(textChannel, message);
                    }
                }
            }
        }
    }

}
