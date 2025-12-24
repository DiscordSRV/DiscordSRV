/*
 * DiscordSRV - https://github.com/DiscordSRV/DiscordSRV
 *
 * Copyright (C) 2016 - 2024 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as
 * published by the Free Software Foundation, either version 3 of the
 * License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public
 * License along with this program.  If not, see
 * <http://www.gnu.org/licenses/gpl-3.0.html>.
 */

package github.scarsz.discordsrv.modules.alerts;

import alexh.weak.Dynamic;
import alexh.weak.Weak;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import net.dv8tion.jda.api.interactions.components.ActionRow;
import net.dv8tion.jda.api.interactions.components.Button;
import net.dv8tion.jda.api.interactions.components.ButtonStyle;
import net.dv8tion.jda.api.events.interaction.ButtonClickEvent;
import org.apache.commons.lang3.StringUtils;
import org.bukkit.event.server.PluginDisableEvent;
import org.apache.commons.lang3.math.NumberUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.event.*;
import org.bukkit.event.player.PlayerCommandPreprocessEvent;
import org.bukkit.event.player.PlayerEvent;
import org.bukkit.event.server.ServerCommandEvent;
import org.bukkit.plugin.RegisteredListener;
import org.jetbrains.annotations.NotNull;
import org.springframework.expression.ParseException;
import org.springframework.expression.spel.SpelEvaluationException;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AlertListener implements Listener, EventListener {

    private static class ButtonAction {
        final String runAs; // "console" or "player"
        final String playerName; // when runAs=player, may be null
        final List<String> commands;
        ButtonAction(String runAs, String playerName, List<String> commands) {
            this.runAs = runAs;
            this.playerName = playerName;
            this.commands = commands;
        }
    }

    private final Map<String, ButtonAction> buttonCommandMap = new ExpiringDualHashBidiMap<>(TimeUnit.MINUTES.toMillis(10));
    private final java.util.Set<PendingDisable> pendingDisables = java.util.concurrent.ConcurrentHashMap.newKeySet();

    private static class PendingDisable {
        final long channelId;
        final long messageId;
        final List<ActionRow> disabledRows;
        PendingDisable(long channelId, long messageId, List<ActionRow> disabledRows) {
            this.channelId = channelId;
            this.messageId = messageId;
            this.disabledRows = disabledRows;
        }
        @Override public boolean equals(Object o) {
            if (this == o) return true;
            if (!(o instanceof PendingDisable)) return false;
            PendingDisable that = (PendingDisable) o;
            return channelId == that.channelId && messageId == that.messageId;
        }
        @Override public int hashCode() { return Objects.hash(channelId, messageId); }
    }

    private static final Pattern VALID_CLASS_NAME_PATTERN = Pattern.compile("([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*");
    private static final List<String> BLACKLISTED_CLASS_NAMES = Arrays.asList(
            // Causes issues with logins with some plugins
            "com.destroystokyo.paper.event.player.PlayerHandshakeEvent",
            // Causes server to on to the main thread & breaks team color on Paper
            "org.bukkit.event.player.PlayerChatEvent",
            // We explicitly listen to these events
            "org.bukkit.event.player.PlayerCommandPreprocessEvent",
            "org.bukkit.event.server.ServerCommandEvent"
    );
    private static final List<String> SYNC_EVENT_NAMES = Arrays.asList(
            // Needs to be sync because block data will be stale by time async task runs
            "org.bukkit.event.block.BlockBreakEvent"
    );

    private static final List<Class<?>> BLACKLISTED_CLASSES = new ArrayList<>();

    private final Map<String, String> validClassNameCache = new ExpiringDualHashBidiMap<>(TimeUnit.MINUTES.toMillis(1));
    private final Set<String> activeTriggers = new HashSet<>();
    private boolean anyCommandTrigger = false;

    static {
        for (String className : BLACKLISTED_CLASS_NAMES) {
            try {
                BLACKLISTED_CLASSES.add(Class.forName(className));
            } catch (ClassNotFoundException ignored) {}
        }
    }

    private final RegisteredListener listener;
    private final List<Dynamic> alerts = new ArrayList<>();
    private boolean registered = false;

    public AlertListener() {
        listener = new RegisteredListener(
                new Listener() {
                    @Override
                    public String toString() {
                        return "DiscordSRV Alerts";
                    }
                },
                (listener, event) -> runAlertsForEvent(event),
                EventPriority.MONITOR,
                DiscordSRV.getPlugin(),
                false
        );
        reloadAlerts();
    }

    public void hackIntoAllHandlerLists() {
        //
        // Bukkit's API has no easy way to listen for all events
        // The best thing you can do is add a listener to all the HandlerList's
        // (and ignore some problematic events)
        //
        // Thus, we have to resort to making a proxy HandlerList.allLists list that adds our listener whenever a new
        // handler list is created by an event being initialized
        //
        try {
            Field allListsField = HandlerList.class.getDeclaredField("allLists");
            allListsField.setAccessible(true);

            if (Modifier.isFinal(allListsField.getModifiers())) {
                try {
                    Field modifiersField = Field.class.getDeclaredField("modifiers");
                    modifiersField.setAccessible(true);
                    modifiersField.setInt(allListsField, allListsField.getModifiers() & ~Modifier.FINAL);
                } catch (NoSuchFieldException ignored) {
                    // No can do
                }
            }

            // set the HandlerList.allLists field to be a proxy list that adds our listener to all initializing lists
            List<HandlerList> existing = HandlerList.getHandlerLists();
            synchronized (existing) {
                for (HandlerList list : existing) {
                    addListener(list);
                }
            }
        } catch (Throwable e) {
            DiscordSRV.error(e);
        }
    }

    private void addListener(HandlerList handlerList) {
        for (Class<?> blacklistedClass : BLACKLISTED_CLASSES) {
            try {
                HandlerList list = (HandlerList) blacklistedClass.getMethod("getHandlerList").invoke(null);
                if (handlerList == list) {
                    DiscordSRV.debug(Debug.ALERTS, "Skipping registering HandlerList for " + blacklistedClass.getName() + " for alerts");
                    return;
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                DiscordSRV.debug(Debug.ALERTS, "Failed to check if HandlerList was for " + blacklistedClass.getName() + ": " + e.toString());
            }
        }
        for (StackTraceElement stackTraceElement : Thread.currentThread().getStackTrace()) {
            String match = BLACKLISTED_CLASS_NAMES.stream().filter(className -> stackTraceElement.getClassName().equals(className)).findAny().orElse(null);
            if (match != null && stackTraceElement.getMethodName().equals("<clinit>")) {
                DiscordSRV.debug(Debug.ALERTS, "Skipping registering HandlerList for " + match + " for alerts (during event init)");
                return;
            }
        }
        if (Arrays.stream(handlerList.getRegisteredListeners()).noneMatch(listener::equals)) handlerList.register(listener);
    }

    public void reloadAlerts() {
        validClassNameCache.clear();
        activeTriggers.clear();
        anyCommandTrigger = false;
        alerts.clear();
        Optional<List<Map<?, ?>>> optionalAlerts = DiscordSRV.config().getOptional("Alerts");
        if (registered) unregister();

        if (!optionalAlerts.isPresent() || optionalAlerts.get().isEmpty()) {
            return;
        }

        List<String> simpleClassNames = new ArrayList<>();
        Set<HandlerList> handlerLists = new HashSet<>();
        long count = optionalAlerts.get().size();

        for (Map<?, ?> map : optionalAlerts.get()) {
            Dynamic alert = Dynamic.from(map);
            alerts.add(alert);
            Set<String> triggers = getTriggers(alert);

            for (String trigger : triggers) {
                if (trigger.startsWith("/")) {
                    anyCommandTrigger = true;
                    continue;
                }
                activeTriggers.add(trigger.toLowerCase(Locale.ROOT));

                if (!trigger.contains(".")) {
                    simpleClassNames.add(trigger);
                    continue;
                }

                try {
                    Class<?> eventClass = Class.forName(getEventClassName(trigger));
                    if (!Event.class.isAssignableFrom(eventClass)) {
                        // Not a Bukkit event ignore (DiscordSRV & JDA events don't need to be registered explicitly)
                        continue;
                    }
                    if (PlayerCommandPreprocessEvent.class.isAssignableFrom(eventClass)
                            || ServerCommandEvent.class.isAssignableFrom(eventClass)) {
                        // Already registered, don't register again
                        continue;
                    }

                    Method method = null;
                    Class<?> handlerListClass = eventClass;
                    while (method == null && handlerListClass != null) {
                        try {
                            method = handlerListClass.getDeclaredMethod("getHandlerList");
                        } catch (NoSuchMethodException ignored) {}
                        handlerListClass = handlerListClass.getSuperclass();
                    }

                    if (method == null) {
                        DiscordSRV.error("Could not find getHandlerList method for " + eventClass.getName());
                        continue;
                    }

                    Object handlerList = method.invoke(null);
                    if (!(handlerList instanceof HandlerList)) {
                        DiscordSRV.error("Could not get HandlerList for " + eventClass.getName() + ": getHandlerList does not actually return a " + HandlerList.class.getName());
                        continue;
                    }

                    if (!handlerLists.add((HandlerList) handlerList)) {
                        // Ignore duplicate HandlerLists
                        continue;
                    }

                    addListener((HandlerList) handlerList);
                } catch (ClassNotFoundException ignored) {
                    DiscordSRV.warning("Could not find event for alert trigger: " + trigger);
                } catch (InvocationTargetException | IllegalAccessException e) {
                    DiscordSRV.error("Could not get HandlerList for event " + trigger, e);
                }
            }
        }

        if (!simpleClassNames.isEmpty()) {
            DiscordSRV.warning("Some alerts are using simple class names as triggers (instead of fully-classified class names including the package name), server performance may be effected.");
            DiscordSRV.warning("Support for simple class names will be removed in a future release of DiscordSRV");
            DiscordSRV.warning("The following triggers are causing this notification: " + String.join(", ", simpleClassNames));
            DiscordSRV.warning("Read https://docs.discordsrv.com/alerts/migration for more information");
            hackIntoAllHandlerLists();
        }
        registered = true;
        DiscordSRV.info(optionalAlerts.get().size() + " alert" + (count > 1 ? "s" : "") + " registered");
    }

    public List<Dynamic> getAlerts() {
        return alerts;
    }

    public void unregister() {
        HandlerList.unregisterAll(listener.getListener());
        registered = false;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        // Handle button interactions for command buttons
        if (event instanceof ButtonClickEvent) {
            ButtonClickEvent btnEvent = (ButtonClickEvent) event;
            String id = btnEvent.getComponentId();
            ButtonAction action = buttonCommandMap.get(id);
            if (action != null) {
                try {
                    // Acknowledge the click to avoid "This interaction failed"
                    btnEvent.reply("Command queued.").setEphemeral(true).queue();
                } catch (Throwable ignored) {}

                // Disable the clicked button immediately on the message (non-webhook path only has message objects we can edit)
                try {
                    List<ActionRow> current = btnEvent.getMessage().getActionRows();
                    List<ActionRow> updated = new ArrayList<>(current.size());
                    boolean changed = false;
                    for (ActionRow row : current) {
                        List<Button> newButtons = new ArrayList<>(row.getButtons().size());
                        for (Button b : row.getButtons()) {
                            if (id.equals(b.getId())) {
                                newButtons.add(b.withDisabled(true));
                                changed = true;
                            } else {
                                newButtons.add(b);
                            }
                        }
                        updated.add(ActionRow.of(newButtons));
                    }
                    if (changed) {
                        // Try edit via event helper if available; fallback to editing the message
                        try {
                            btnEvent.getMessage().editMessageComponents(updated).queue(s -> {}, e -> {});
                        } catch (Throwable ignored2) {}
                    }
                } catch (Throwable t) {
                    DiscordSRV.debug(Debug.ALERTS, "Failed to disable clicked button immediately: " + t.getMessage());
                }

                // Remove mapping to prevent any reuse
                buttonCommandMap.remove(id);

                // Execute commands according to action context on the main thread
                SchedulerUtil.runTask(DiscordSRV.getPlugin(), () -> {
                    try {
                        if ("player".equalsIgnoreCase(action.runAs)) {
                            Player p = action.playerName != null ? Bukkit.getPlayerExact(action.playerName) : null;
                            for (String c : action.commands) {
                                boolean ok;
                                if (p != null && p.isOnline()) {
                                    ok = Bukkit.dispatchCommand(p, c);
                                    if (!ok) DiscordSRV.warning("Player command from button failed to dispatch: " + c);
                                } else {
                                    ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
                                    if (!ok) DiscordSRV.warning("Fallback console command from button failed to dispatch: " + c);
                                }
                            }
                        } else {
                            for (String c : action.commands) {
                                boolean ok = Bukkit.dispatchCommand(Bukkit.getConsoleSender(), c);
                                if (!ok) DiscordSRV.warning("Command from button failed to dispatch: " + c);
                            }
                        }
                    } catch (Throwable t) {
                        DiscordSRV.error("Error running button commands: " + t.getMessage());
                    }
                });
                return; // handled
            }
        }
        runAlertsForEvent(event);
    }

    @Subscribe
    public void onDSRVEvent(github.scarsz.discordsrv.api.events.Event event) {
        runAlertsForEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent event) {
        runAlertsForEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onServerCommand(ServerCommandEvent event) {
        runAlertsForEvent(event);
    }

    @EventHandler(priority = EventPriority.MONITOR)
    public void onPluginDisable(PluginDisableEvent event) {
        try {
            if (event.getPlugin() != DiscordSRV.getPlugin()) return;
            if (pendingDisables.isEmpty()) return;
            // Copy to avoid concurrent modification if callbacks remove entries
            List<PendingDisable> copy = new ArrayList<>(pendingDisables);
            for (PendingDisable pd : copy) {
                try {
                    TextChannel ch = DiscordUtil.getJda().getTextChannelById(pd.channelId);
                    if (ch == null) {
                        pendingDisables.remove(pd);
                        continue;
                    }
                    ch.retrieveMessageById(pd.messageId).queue(msg -> {
                        try {
                            msg.editMessageComponents(pd.disabledRows).queue(s -> pendingDisables.remove(pd), e -> pendingDisables.remove(pd));
                        } catch (Throwable ignored) {
                            pendingDisables.remove(pd);
                        }
                    }, err -> pendingDisables.remove(pd));
                } catch (Throwable ignored) {
                    pendingDisables.remove(pd);
                }
            }
        } catch (Throwable t) {
            DiscordSRV.debug(Debug.ALERTS, "Plugin disable button cleanup failed: " + t.getMessage());
        }
    }

    private void runAlertsForEvent(Object event) {
        boolean command = event instanceof PlayerCommandPreprocessEvent || event instanceof ServerCommandEvent;

        String eventClassName = getEventClassName(event);
        boolean active = (command && anyCommandTrigger)
                || activeTriggers.contains(eventClassName.toLowerCase(Locale.ROOT))
                || activeTriggers.contains(getEventName(event).toLowerCase(Locale.ROOT));
        if (!active) {
            if (event instanceof Event) {
                // remove us from HandlerLists that we don't need (we can do this here, since we have the full class name)
                // but we need to ignore events where the HandlerList may be inherited from a super class
                Class<?> checkClass = event.getClass().getSuperclass();

                boolean anySuperClassHasHandlersMethod = false;
                while (checkClass != null) {
                    try {
                        checkClass.getDeclaredMethod("getHandlers");
                        anySuperClassHasHandlersMethod = true;
                        break;
                    } catch (NoSuchMethodException ignored) {}

                    checkClass = checkClass.getSuperclass();
                }

                if (!anySuperClassHasHandlersMethod) {
                    HandlerList handlerList = ((Event) event).getHandlers();
                    handlerList.unregister(this);
                }
            }
            return;
        }

        for (int i = 0; i < alerts.size(); i++) {
            Dynamic alert = alerts.get(i);
            Set<String> triggers = getTriggers(alert);
            boolean async = true;

            Dynamic asyncDynamic = alert.get("Async");
            if (asyncDynamic.isPresent()) {
                if (asyncDynamic.convert().intoString().equalsIgnoreCase("false")
                        || asyncDynamic.convert().intoString().equalsIgnoreCase("no")) {
                    async = false;
                }
            }

            for (String syncName : SYNC_EVENT_NAMES) {
                if (eventClassName.equals(syncName)) {
                    async = false;
                    break;
                }
            }

            if (async) {
                int alertIndex = i;
                SchedulerUtil.runTaskAsynchronously(DiscordSRV.getPlugin(), () -> process(event, alert, triggers, alertIndex));
            } else {
                process(event, alert, triggers, i);
            }
        }
    }

    private Set<String> getTriggers(Dynamic alert) {
        Set<String> triggers = new HashSet<>();
        Dynamic triggerDynamic = alert.get("Trigger");
        if (triggerDynamic.isList()) {
            triggers.addAll(triggerDynamic.children()
                    .map(Weak::asString)
                    .collect(Collectors.toSet())
            );
        } else if (triggerDynamic.isString()) {
            triggers.add(triggerDynamic.asString());
        }

        Set<String> finalTriggers = new HashSet<>();
        for (String trigger : triggers) {
            if (!trigger.startsWith("/")) {
                String className = validClassNameCache.get(trigger);
                if (className == null) {
                    // event trigger, make sure it's a valid class name
                    Matcher matcher = VALID_CLASS_NAME_PATTERN.matcher(trigger);
                    if (matcher.find()) {
                        // valid class name found
                        className = matcher.group();
                    }
                    validClassNameCache.put(trigger, className);
                }
                finalTriggers.add(className);
                continue;
            }
            finalTriggers.add(trigger);
        }
        return finalTriggers;
    }

    private String getEventClassName(Object event) {
        String className = event instanceof String ? (String) event : event.getClass().getName();
        return className.replace("github.scarsz.discordsrv.dependencies.jda", "net.".concat("dv8tion.jda"));
    }

    private String getEventName(Object event) {
        return event instanceof Event ? ((Event) event).getEventName() : event.getClass().getSimpleName();
    }

    private void process(Object event, Dynamic alert, Set<String> triggers, int alertIndex) {
        Player player = event instanceof PlayerEvent ? ((PlayerEvent) event).getPlayer() : null;
        if (player == null) {
            // some things that do deal with players are not properly marked as a player event
            // this will check to see if a #getPlayer() method exists on events coming through
            try {
                Method getPlayerMethod = event.getClass().getMethod("getPlayer");
                if (getPlayerMethod.getReturnType().equals(Player.class)) {
                    player = (Player) getPlayerMethod.invoke(event);
                }
            } catch (Exception ignored) {
                // we tried ¯\_(ツ)_/¯
            }
        }

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

        MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("Alerts." + alertIndex);

        String eventClassName = getEventClassName(event);
        String eventName = getEventName(event);
        for (String trigger : triggers) {
            if (trigger.startsWith("/")) {
                if (StringUtils.isBlank(command) || !command.toLowerCase().split("\\s+|$", 2)[0].equals(trigger.substring(1))) continue;
            } else {
                // make sure the called event matches what this alert is supposed to trigger on
                if (!eventClassName.equals(trigger) && !eventName.equalsIgnoreCase(trigger)) continue;
            }

            // make sure alert should run even if event is cancelled
            if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                Dynamic ignoreCancelledDynamic = alert.get("IgnoreCancelled");
                boolean ignoreCancelled = ignoreCancelledDynamic.isPresent() ? ignoreCancelledDynamic.as(Boolean.class) : true;
                if (ignoreCancelled) {
                    DiscordSRV.debug(Debug.ALERTS, "Not running alert for event " + eventName + ": event was cancelled");
                    return;
                }
            }

            Dynamic textChannelsDynamic = alert.get("Channel");
            if (textChannelsDynamic == null) {
                DiscordSRV.debug(Debug.ALERTS, "Not running alert for trigger " + trigger + ": no target channel was defined");
                return;
            }
            Set<String> channels = new HashSet<>();
            if (textChannelsDynamic.isList()) {
                textChannelsDynamic.children()
                        .map(Weak::asString)
                        .filter(Objects::nonNull)
                        .forEach(channels::add);
            } else if (textChannelsDynamic.isString()) {
                channels.add(textChannelsDynamic.asString());
            }
            Function<Function<String, Collection<TextChannel>>, Set<TextChannel>> channelResolver = converter -> {
                Set<TextChannel> textChannels = new HashSet<>();
                channels.forEach(channel -> textChannels.addAll(converter.apply(channel)));
                textChannels.removeIf(Objects::isNull);
                return textChannels;
            };

            Set<TextChannel> textChannels = channelResolver.apply(s -> {
                TextChannel target = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(s);
                return Collections.singleton(target);
            });
            if (textChannels.isEmpty()) {
                textChannels.addAll(channelResolver.apply(s ->
                        DiscordUtil.getJda().getTextChannelsByName(s, false)
                ));
            }
            if (textChannels.isEmpty()) {
                textChannels.addAll(channelResolver.apply(s -> NumberUtils.isDigits(s) ?
                        Collections.singleton(DiscordUtil.getJda().getTextChannelById(s)) : Collections.emptyList()));
            }

            if (textChannels.size() == 0) {
                DiscordSRV.debug(Debug.ALERTS, "Not running alert for trigger " + trigger + ": no target channel was defined/found (channels: " + channels + ")");
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
                        try {
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
                            DiscordSRV.debug(Debug.ALERTS, "Condition \"" + expression + "\" -> " + value);
                            if (value != null && !value) {
                                allConditionsMet = false;
                                break;
                            }
                        } catch (ParseException e) {
                            DiscordSRV.error("Error while parsing expression \"" + expression + "\" for trigger \"" + trigger + "\" -> " + e.getMessage());
                        } catch (SpelEvaluationException e) {
                            DiscordSRV.error("Error while evaluating expression \"" + expression + "\" for trigger \"" + trigger + "\" -> " + e.getMessage());
                        }
                    }
                    if (!allConditionsMet) continue;
                }

                CommandSender finalSender = sender;
                String finalCommand = command;

                Player finalPlayer = player;
                BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
                    if (content == null) return null;

                    // evaluate any SpEL expressions
                    Map<String, Object> variables = new HashMap<>();
                    variables.put("event", event);
                    variables.put("server", Bukkit.getServer());
                    variables.put("discordsrv", DiscordSRV.getPlugin());
                    variables.put("player", finalPlayer);
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
                                return finalPlayer != null ? PlayerUtil.getPing(finalPlayer) : "-1";
                            case "name":
                            case "username":
                                return finalPlayer != null ? finalPlayer.getName() : "";
                            case "displayname":
                                return finalPlayer != null ? MessageUtil.strip(needsEscape ? DiscordUtil.escapeMarkdown(finalPlayer.getDisplayName()) : finalPlayer.getDisplayName()) : "";
                            case "world":
                                return finalPlayer != null ? finalPlayer.getWorld().getName() : "";
                            case "embedavatarurl":
                                return finalPlayer != null ? DiscordSRV.getAvatarUrl(finalPlayer) : DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
                            case "botavatarurl":
                                return DiscordUtil.getJda().getSelfUser().getEffectiveAvatarUrl();
                            case "botname":
                                return DiscordSRV.getPlugin().getMainGuild() != null ? DiscordSRV.getPlugin().getMainGuild().getSelfMember().getEffectiveName() : DiscordUtil.getJda().getSelfUser().getName();
                            default:
                                return "{" + key + "}";
                        }
                    });

                    content = DiscordUtil.translateEmotes(content, textChannel.getGuild());
                    content = PlaceholderUtil.replacePlaceholdersToDiscord(content, finalPlayer);
                    return content;
                };

                Message message = DiscordSRV.translateMessage(messageFormat, translator);
                if (message == null) {
                    DiscordSRV.debug(Debug.ALERTS, "Not sending alert because it is configured to have no message content");
                    return;
                }

                // Build buttons (link or command) into action rows if configured
                List<ActionRow> actionRows = new ArrayList<>();
                Dynamic buttonsDynamic = alert.get("Buttons");
                if (buttonsDynamic != null && buttonsDynamic.isList()) {
                    List<Button> builtButtons = new ArrayList<>();
                    Iterator<Dynamic> it = buttonsDynamic.children().iterator();
                    while (it.hasNext()) {
                        Dynamic btn = it.next();
                        try {
                            String label = translator.apply(btn.dget("Text").asString(), false);
                            String url = btn.get("Url").isPresent() ? translator.apply(btn.dget("Url").asString(), true) : null;
                            String styleStr = btn.get("Style").isPresent() ? btn.dget("Style").asString() : null;
                            String runAs = btn.get("RunAs").isPresent() ? btn.dget("RunAs").asString() : "console";

                            if (StringUtils.isBlank(label)) continue;

                            if (StringUtils.isNotBlank(url)) {
                                // URL provided: must be a link button; style ignored/forced to LINK
                                Button button = Button.link(url, label);
                                builtButtons.add(button);
                                continue;
                            }

                            // Gather commands: support either a single Command (string) or Commands (list)
                            List<String> commandsList = new ArrayList<>();
                            if (btn.get("Commands").isPresent() && btn.dget("Commands").isList()) {
                                Iterator<Dynamic> cit = btn.dget("Commands").children().iterator();
                                while (cit.hasNext()) {
                                    Dynamic cd = cit.next();
                                    String cmd = translator.apply(cd.convert().intoString(), false);
                                    if (StringUtils.isNotBlank(cmd)) commandsList.add(cmd);
                                }
                            }
                            if (commandsList.isEmpty() && btn.get("Command").isPresent()) {
                                String single = translator.apply(btn.dget("Command").asString(), false);
                                if (StringUtils.isNotBlank(single)) commandsList.add(single);
                            }

                            // Command/custom-id button must have at least one command
                            if (commandsList.isEmpty()) {
                                DiscordSRV.debug(Debug.ALERTS, "Skipping button with no Url/Command(s) in alert index " + alertIndex);
                                continue;
                            }

                            // Map style string to ButtonStyle (cannot be LINK for custom-id buttons)
                            ButtonStyle style = ButtonStyle.PRIMARY;
                            if (StringUtils.isNotBlank(styleStr)) {
                                switch (styleStr.trim().toLowerCase(Locale.ROOT)) {
                                    case "primary": style = ButtonStyle.PRIMARY; break;
                                    case "secondary": style = ButtonStyle.SECONDARY; break;
                                    case "success": style = ButtonStyle.SUCCESS; break;
                                    case "danger": style = ButtonStyle.DANGER; break;
                                    case "link":
                                        DiscordSRV.debug(Debug.ALERTS, "Ignoring LINK style for non-url button in alert index " + alertIndex + ", using PRIMARY");
                                        style = ButtonStyle.PRIMARY; break;
                                }
                            }

                            // Create a custom id and store mapping to ButtonAction (+context for runAs)
                            String customId = UUID.randomUUID().toString();
                            String playerName = ("player".equalsIgnoreCase(runAs) && finalPlayer != null) ? finalPlayer.getName() : null;
                            buttonCommandMap.put(customId, new ButtonAction(runAs, playerName, commandsList));

                            Button button = Button.of(style, customId, label);
                            builtButtons.add(button);
                        } catch (Throwable t) {
                            DiscordSRV.debug(Debug.ALERTS, "Skipping invalid button in alert index " + alertIndex + ": " + t.getMessage());
                        }
                    }
                    // Discord allows 5 rows max, 5 buttons per row; cap at 25
                    int limit = Math.min(25, builtButtons.size());
                    for (int i = 0; i < limit; i += 5) {
                        List<Button> slice = builtButtons.subList(i, Math.min(i + 5, limit));
                        if (!slice.isEmpty()) actionRows.add(ActionRow.of(slice));
                        if (actionRows.size() >= 5) break;
                    }
                }

                if (messageFormat.isUseWebhooks()) {
                    if (!actionRows.isEmpty()) {
                        WebhookUtil.deliverMessage(textChannel,
                                translator.apply(messageFormat.getWebhookName(), false),
                                translator.apply(messageFormat.getWebhookAvatarUrl(), false),
                                message.getContentRaw(), message.getEmbeds().stream().findFirst().orElse(null), null, actionRows);
                    } else {
                        WebhookUtil.deliverMessage(textChannel,
                                translator.apply(messageFormat.getWebhookName(), false),
                                translator.apply(messageFormat.getWebhookAvatarUrl(), false),
                                message.getContentRaw(), message.getEmbeds().stream().findFirst().orElse(null));
                    }
                } else {
                    if (!actionRows.isEmpty()) {
                        try {
                            // Prepare disabled copies of the components to apply after 10 minutes
                            List<ActionRow> disabledRows = new ArrayList<>();
                            for (ActionRow row : actionRows) {
                                List<Button> disabledButtons = row.getButtons().stream()
                                        // Only disable command buttons; keep link buttons enabled
                                        .map(b -> b.getStyle() == ButtonStyle.LINK ? b : b.withDisabled(true))
                                        .collect(Collectors.toList());
                                disabledRows.add(ActionRow.of(disabledButtons));
                            }

                            textChannel.sendMessage(message).setActionRows(actionRows).queue(sent -> {
                                try {
                                    // Track this message for potential shutdown-time disable
                                    PendingDisable pd = new PendingDisable(sent.getChannel().getIdLong(), sent.getIdLong(), disabledRows);
                                    pendingDisables.add(pd);

                                    // Disable buttons after 10 minutes to reflect interaction expiry
                                    sent.editMessageComponents(disabledRows).queueAfter(10, TimeUnit.MINUTES, s -> {
                                        pendingDisables.remove(pd);
                                    }, err -> {
                                        pendingDisables.remove(pd);
                                    });
                                } catch (Throwable ignored) {}
                            }, err -> {
                                // fallback without buttons on failure
                                DiscordSRV.error("Failed to send alert with buttons: " + err.getMessage());
                                DiscordUtil.queueMessage(textChannel, message);
                            });
                        } catch (Exception e) {
                            DiscordSRV.error("Failed to send alert with buttons: " + e.getMessage());
                            // fallback without buttons
                            DiscordUtil.queueMessage(textChannel, message);
                        }
                    } else {
                        DiscordUtil.queueMessage(textChannel, message);
                    }
                }
            }
        }
    }

}
