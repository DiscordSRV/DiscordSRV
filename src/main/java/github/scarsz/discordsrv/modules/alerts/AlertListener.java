package github.scarsz.discordsrv.modules.alerts;

import alexh.weak.Dynamic;
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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.stream.Collectors;

public class AlertListener implements Listener {

    static {
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
            field.set(null, new ArrayList<HandlerList>((List<HandlerList>) field.get(null)) {
                @Override
                public boolean add(HandlerList list) {
                    boolean added = super.add(list);
                    if (Arrays.stream(list.getRegisteredListeners()).noneMatch(listener::equals)) list.register(listener);
                    return added;
                }
            });
        } catch(NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
    }

    private static RegisteredListener listener;

    public void register() {
        listener = new RegisteredListener(
                new Listener() {},
                (listener, event) -> onEvent(event),
                EventPriority.MONITOR,
                DiscordSRV.getPlugin(),
                false
        );

        long count = DiscordSRV.config().dget("Alerts").children().count();
        if (count > 0) DiscordSRV.info(count + " alert" + (count > 1 ? "s" : "") + " registered");
    }

    public void unregister() {
        for (HandlerList handlerList : HandlerList.getHandlerLists()) {
            handlerList.unregister(listener);
        }
    }

    public <E extends Event> void onEvent(E event) {
        Player player = event instanceof PlayerEvent ? ((PlayerEvent) event).getPlayer() : null;
        CommandSender sender = null;
        String command = null;

        if (event instanceof PlayerCommandPreprocessEvent) {
            sender = player;
            command = ((PlayerCommandPreprocessEvent) event).getMessage().substring(1);
        } else if (event instanceof ServerCommandEvent) {
            sender = ((ServerCommandEvent) event).getSender();
            command = ((ServerCommandEvent) event).getCommand();
        }

        List<Dynamic> alerts = DiscordSRV.config().dget("Alerts").children().collect(Collectors.toList());
        for (int i = 0; i < alerts.size(); i++) {
            Dynamic alert = alerts.get(i);
            String triggerEvent = alert.get("Trigger").convert().intoString();

            // make sure the called event matches what this alert is supposed to trigger on
            if (!event.getEventName().equalsIgnoreCase(triggerEvent)) return;

            // make sure channel is available
            String gameChannel = alert.get("Channel").asString();
            if (gameChannel == null) {
                DiscordSRV.debug("Not running alert for trigger " + triggerEvent + ": no target channel was defined");
                return;
            }
            TextChannel channel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(gameChannel);
            if (channel == null) {
                DiscordSRV.debug("Not running alert for trigger " + triggerEvent + ": target TextChannel was not available");
                return;
            }

            // make sure alert should run even if event is cancelled
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                Boolean ignoreCancelled = alert.get("IgnoreCancelled").as(Boolean.class);
                if (ignoreCancelled == null || ignoreCancelled) {
                    DiscordSRV.debug("Not running alert for trigger " + triggerEvent + ": event was cancelled");
                    return;
                }
            }

            // check alert conditions
            Iterator<Dynamic> conditions = alert.dget("Conditions").children().iterator();
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
                        .withVariable("channel", channel)
                        .withVariable("jda", DiscordUtil.getJda())
                        .evaluate(event, Boolean.class);
                DiscordSRV.debug("Condition \"" + expression + "\" -> " + value);
                if (value != null && !value) {
                    return;
                }
            }

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

}
