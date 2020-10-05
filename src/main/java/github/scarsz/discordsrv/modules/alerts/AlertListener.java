package github.scarsz.discordsrv.modules.alerts;

import alexh.weak.Dynamic;
import alexh.weak.Weak;
import github.scarsz.discordsrv.Debug;
import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.objects.ExpiringDualHashBidiMap;
import github.scarsz.discordsrv.objects.Lag;
import github.scarsz.discordsrv.objects.MessageFormat;
import github.scarsz.discordsrv.util.*;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.GenericEvent;
import net.dv8tion.jda.api.hooks.EventListener;
import org.apache.commons.lang3.StringUtils;
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
import java.lang.reflect.Modifier;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class AlertListener implements Listener, EventListener {

    private static final List<String> BLACKLISTED_CLASS_NAMES = Arrays.asList(
            // Causes issues with logins with some plugins
            "com.destroystokyo.paper.event.player.PlayerHandshakeEvent",
            // Causes server to on to the main thread & breaks team color on Paper
            "org.bukkit.event.player.PlayerChatEvent"
    );
    private static final List<Class<?>> BLACKLISTED_CLASSES = new ArrayList<>();

    private static final Pattern VALID_CLASS_NAME_PATTERN = Pattern.compile("([\\p{L}_$][\\p{L}\\p{N}_$]*\\.)*[\\p{L}_$][\\p{L}\\p{N}_$]*");
    private final Map<String, String> validClassNameCache = new ExpiringDualHashBidiMap<>(TimeUnit.MINUTES.toMillis(1));

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
                this,
                (listener, event) -> onEvent(event),
                EventPriority.MONITOR,
                DiscordSRV.getPlugin(),
                false
        );
        reloadAlerts();
    }

    public void register() {
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
                Field modifiersField = Field.class.getDeclaredField("modifiers");
                modifiersField.setAccessible(true);
                modifiersField.setInt(allListsField, allListsField.getModifiers() & ~Modifier.FINAL);
            }

            // set the HandlerList.allLists field to be a proxy list that adds our listener to all initializing lists
            allListsField.set(null, new ArrayList<HandlerList>(HandlerList.getHandlerLists()) {
                {
                    // add any already existing handler lists to our new proxy list
                    synchronized (this) {
                        this.addAll(HandlerList.getHandlerLists());
                    }
                }

                @Override
                public boolean addAll(Collection<? extends HandlerList> c) {
                    boolean changed = false;
                    for (HandlerList handlerList : c) {
                        if (add(handlerList)) changed = true;
                    }
                    return changed;
                }

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
        registered = true;
    }

    private void addListener(HandlerList handlerList) {
        for (Class<?> blacklistedClass : BLACKLISTED_CLASSES) {
            try {
                HandlerList list = (HandlerList) blacklistedClass.getMethod("getHandlerList").invoke(null);
                if (handlerList == list) {
                    DiscordSRV.debug("Skipping registering HandlerList for " + blacklistedClass.getName() + " for alerts");
                    return;
                }
            } catch (NoSuchMethodException | InvocationTargetException | IllegalAccessException e) {
                DiscordSRV.debug("Failed to check if HandlerList was for " + blacklistedClass.getName() + ": " + e.toString());
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
        alerts.clear();
        Optional<List<Map<?, ?>>> optionalAlerts = DiscordSRV.config().getOptional("Alerts");
        boolean any = optionalAlerts.isPresent() && !optionalAlerts.get().isEmpty();
        if (any) {
            if (!registered) register();
            long count = optionalAlerts.get().size();
            DiscordSRV.info(optionalAlerts.get().size() + " alert" + (count > 1 ? "s" : "") + " registered");

            for (Map<?, ?> map : optionalAlerts.get()) {
                alerts.add(Dynamic.from(map));
            }
        } else if (registered) {
            unregister();
        }
    }

    public List<Dynamic> getAlerts() {
        return alerts;
    }

    public void unregister() {
        HandlerList.unregisterAll(this);
        registered = false;
    }

    @Override
    public void onEvent(@NotNull GenericEvent event) {
        onEvent((Object) event);
    }

    private void onEvent(Object event) {
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

            triggers = triggers.stream()
                    .map(s -> {
                        if (!s.startsWith("/")) {
                            String className = validClassNameCache.get(s);
                            if (className == null) {
                                // event trigger, make sure it's a valid class name
                                Matcher matcher = VALID_CLASS_NAME_PATTERN.matcher(s);
                                if (matcher.find()) {
                                    // valid class name found
                                    className = matcher.group();
                                }
                                validClassNameCache.put(s, className);
                            }
                            return className;
                        }
                        return s;
                    })
                    .collect(Collectors.toSet());

            for (String trigger : triggers) {
                String eventName = (event instanceof Event ? ((Event) event).getEventName() : event.getClass().getSimpleName());
                if (trigger.startsWith("/")) {
                    if (StringUtils.isBlank(command) || !command.toLowerCase().split("\\s+|$", 2)[0].equals(trigger.substring(1))) continue;
                } else {
                    // make sure the called event matches what this alert is supposed to trigger on
                    if (!eventName.equalsIgnoreCase(trigger)) continue;
                }

                // make sure alert should run even if event is cancelled
                if (event instanceof Cancellable && ((Cancellable) event).isCancelled()) {
                    Dynamic ignoreCancelledDynamic = alert.get("IgnoreCancelled");
                    boolean ignoreCancelled = ignoreCancelledDynamic.isPresent() ? ignoreCancelledDynamic.as(boolean.class) : true;
                    if (ignoreCancelled) {
                        DiscordSRV.debug(Debug.ALERTS, "Not running alert for event " + eventName + ": event was cancelled");
                        return;
                    }
                }

                Set<TextChannel> textChannels = new HashSet<>();
                Dynamic textChannelsDynamic = alert.get("Channel");
                if (textChannelsDynamic == null) {
                    DiscordSRV.debug(Debug.ALERTS, "Not running alert for trigger " + trigger + ": no target channel was defined");
                    return;
                }
                if (textChannelsDynamic.isList()) {
                    Function<Function<String, TextChannel>, Set<TextChannel>> channelResolver = converter ->
                            textChannelsDynamic.children()
                                    .map(Weak::asString)
                                    .map(converter)
                                    .filter(Objects::nonNull)
                                    .collect(Collectors.toSet());

                    Set<TextChannel> channels = channelResolver.apply(s -> {
                        TextChannel target = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(s);
                        if (target == null) {
                            DiscordSRV.debug(Debug.ALERTS, "Not sending alert for trigger " + trigger + " to target channel "
                                    + s + ": TextChannel was not available");
                        }
                        return target;
                    });
                    if (channels.isEmpty()) {
                        channels.addAll(channelResolver.apply(s ->
                                DiscordUtil.getJda().getTextChannelsByName(s, false)
                                        .stream().findFirst().orElse(null)
                        ));
                    }
                } else if (textChannelsDynamic.isString()) {
                    String channelName = textChannelsDynamic.asString();
                    TextChannel textChannel = DiscordSRV.getPlugin().getDestinationTextChannelForGameChannelName(channelName);
                    if (textChannel != null) {
                        textChannels.add(textChannel);
                    } else {
                        DiscordUtil.getJda().getTextChannelsByName(channelName, false)
                                .stream().findFirst().ifPresent(textChannels::add);
                    }
                }
                textChannels.removeIf(Objects::isNull);
                if (textChannels.size() == 0) {
                    DiscordSRV.debug(Debug.ALERTS, "Not running alert for trigger " + trigger + ": no target channel was defined");
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
                    MessageFormat messageFormat = DiscordSRV.getPlugin().getMessageFromConfiguration("Alerts." + i);

                    BiFunction<String, Boolean, String> translator = (content, needsEscape) -> {
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
                    };

                    Message message = DiscordSRV.getPlugin().translateMessage(messageFormat, translator);

                    if (messageFormat.isUseWebhooks()) {
                        WebhookUtil.deliverMessage(textChannel,
                                translator.apply(messageFormat.getWebhookName(), false),
                                translator.apply(messageFormat.getWebhookAvatarUrl(), false),
                                message.getContentRaw(), message.getEmbeds().stream().findFirst().orElse(null));
                    } else {
                        DiscordUtil.queueMessage(textChannel, message);
                    }
                }
            }
        }
    }
}
