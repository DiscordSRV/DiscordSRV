package github.scarsz.discordsrv.voice;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.PlayerUtil;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.*;
import net.dv8tion.jda.core.events.guild.voice.GuildVoiceJoinEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import net.dv8tion.jda.core.managers.GuildController;
import net.dv8tion.jda.core.managers.PermOverrideManager;
import net.dv8tion.jda.core.requests.restaction.PermissionOverrideAction;
import org.bukkit.Bukkit;
import org.bukkit.OfflinePlayer;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.event.player.PlayerMoveEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.event.player.PlayerTeleportEvent;

import java.util.*;
import java.util.stream.Collectors;

public class VoiceModule extends ListenerAdapter implements Listener {

    public VoiceModule() {
        if (DiscordSRV.config().getBoolean("Voice enabled")) {
            DiscordSRV.getPlugin().getJda().addEventListener(this);
            Bukkit.getPluginManager().registerEvents(this, DiscordSRV.getPlugin());
            Bukkit.getScheduler().runTaskLater(DiscordSRV.getPlugin(), () ->
                    Bukkit.getScheduler().runTaskTimerAsynchronously(
                            DiscordSRV.getPlugin(),
                            this::tick,
                            0,
                            DiscordSRV.config().getInt("Tick speed")
                    ),
                    0
            );
        }

        Category category = DiscordSRV.getPlugin().getJda().getCategoryById(DiscordSRV.config().getString("Voice category"));
        if (category != null) {
            category.getVoiceChannels().stream()
                    .filter(channel -> {
                        try {
                            //noinspection ResultOfMethodCallIgnored
                            UUID.fromString(channel.getName());
                            return true;
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .forEach(channel -> channel.delete().reason("Orphan").queue());
        }
    }

    private final Set<Player> dirtyPlayers = new HashSet<>();
    private final Set<Network> networks = new HashSet<>();

    private void tick() {
        if (getCategory() == null) {
            DiscordSRV.debug("Skipping voice module tick, category is null");
            return;
        }
        if (getLobbyChannel() == null) {
            DiscordSRV.debug("Skipping voice module tick, lobby channel is null");
            return;
        }

        // remove networks that have no voice channel
        new ArrayList<>(networks).stream()
                .filter(network -> network.getChannel() == null)
                .forEach(Network::die);

        checkPermissions();

//        getCategory().getVoiceChannels().stream()
//                .filter(channel -> {
//                    try {
//                        //noinspection ResultOfMethodCallIgnored
//                        UUID.fromString(channel.getName());
//                        return true;
//                    } catch (Exception e) {
//                        return false;
//                    }
//                })
//                .filter(channel -> networks.stream().noneMatch(network -> network.getChannel().equals(channel)))
//                .forEach(channel -> {
//                    DiscordSRV.debug("Deleting network " + channel + ", no members");
//                    channel.delete().reason("Orphan").queue();
//                });

        synchronized (dirtyPlayers) {
            for (Player player : dirtyPlayers) {
                DiscordSRV.debug("Dirty: " + player.getName());

                // if player is in lobby, move them to the network that they might already be in
                networks.stream()
                        .filter(network -> network.getPlayers().contains(player))
                        .forEach(network -> {
                            Member member = getMember(player);
                            if (member != null &&
                                    !network.getChannel().getMembers().contains(member) &&
                                    member.getVoiceState().inVoiceChannel()) {
                                DiscordSRV.debug("Player " + player.getName() + " is in the lobby but they're in a network, connecting");
                                network.connect(player);
                            }
                        });

                // add player to networks that they may have came into contact with
                networks.stream()
                        .filter(network -> network.playerIsInConnectionRange(player))
                        .reduce((network1, network2) -> {
                            if (network1.getPlayers().size() > network2.getPlayers().size()) {
                                network1.engulf(network2);
                                return network1;
                            } else {
                                network2.engulf(network1);
                                return network2;
                            }
                        })
                        .filter(network -> !network.getPlayers().contains(player))
                        .ifPresent(network -> {
                            DiscordSRV.debug(player.getName() + " has entered network " + network + "'s influence, connecting");
                            network.connect(player);
                        });

                // remove player from networks that they lost connection to
                networks.stream()
                        .filter(network -> network.getPlayers().contains(player))
                        .filter(network -> !network.playerIsInRange(player))
                        .collect(Collectors.toSet()) // needed to prevent concurrent modifications
                        .forEach(network -> {
                            DiscordSRV.debug("Player " + player.getName() + " lost connection to " + network + ", disconnecting");
                            network.disconnect(player);
                        });

                // create networks if two players are within activation distance
                Set<Player> playersWithinRange = PlayerUtil.getOnlinePlayers().stream()
                        .filter(p -> networks.stream().noneMatch(network -> network.getPlayers().contains(p)))
                        .filter(p -> !p.equals(player))
                        .filter(p -> p.getWorld().getName().equals(player.getWorld().getName()))
                        .filter(p -> p.getLocation().distance(player.getLocation()) < getStrength())
                        .collect(Collectors.toSet());
                if (playersWithinRange.size() > 0) {
                    if (getCategory().getChannels().size() == 50) {
                        DiscordSRV.debug("Can't create new voice network because category " + getCategory().getName() + " is full of channels");
                        return;
                    }

                    try {
                        Network network = Network.with(playersWithinRange);
                        network.connect(player);
                        this.networks.add(network);
                    } catch (Exception e) {
                        DiscordSRV.error("Failed to create new voice network: " + e.getMessage());
                    }
                }
            }

            dirtyPlayers.clear();
        }
    }

    public void shutdown() {
        this.networks.forEach(Network::die);
    }

    private void checkPermissions() {
        checkCategoryPermissions();
        checkLobbyPermissions();
        networks.forEach(this::checkNetworkPermissions);
    }
    private void checkCategoryPermissions() {
        PermissionOverride override = getCategory().getPermissionOverride(getGuild().getPublicRole());
        if (override == null) {
            getCategory().createPermissionOverride(getGuild().getPublicRole())
                    .setDeny(Permission.VOICE_CONNECT)
                    .queue(null, (throwable) ->
                            DiscordSRV.error("Failed to create permission override for category " + getCategory().getName() + ": " + throwable.getMessage())
                    );
        } else {
            if (!override.getDenied().contains(Permission.VOICE_CONNECT)) {
                override.getManager().deny(Permission.VOICE_CONNECT).complete();
            }
        }
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkLobbyPermissions() {
        PermissionOverride override = getLobbyChannel().getPermissionOverride(getGuild().getPublicRole());
        if (override == null) {
            getLobbyChannel().createPermissionOverride(getGuild().getPublicRole())
                    .setAllow(Permission.VOICE_CONNECT)
                    .setDeny(Permission.VOICE_SPEAK)
                    .queue(null, (throwable) ->
                            DiscordSRV.error("Failed to create permission override for lobby channel " + getLobbyChannel().getName() + ": " + throwable.getMessage())
                    );
        } else {
            PermOverrideManager manager = override.getManager();
            boolean dirty = false;
            if (!override.getAllowed().contains(Permission.VOICE_CONNECT)) {
                manager.grant(Permission.VOICE_CONNECT);
                dirty = true;
            }
            if (!override.getDenied().contains(Permission.VOICE_SPEAK)) {
                manager.deny(Permission.VOICE_SPEAK);
                dirty = true;
            }
            if (dirty) manager.complete();
        }
    }
    @SuppressWarnings("ResultOfMethodCallIgnored")
    private void checkNetworkPermissions(Network network) {
        PermissionOverride override = network.getChannel().getPermissionOverride(getGuild().getPublicRole());
        if (override == null) {
            PermissionOverrideAction action = network.getChannel().createPermissionOverride(getGuild().getPublicRole());
            if (isVoiceActivationAllowed()) {
                action.setAllow(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD);
                action.setDeny(Permission.VOICE_CONNECT);
            } else {
                action.setAllow(Permission.VOICE_SPEAK);
                action.setDeny(Permission.VOICE_CONNECT, Permission.VOICE_USE_VAD);
            }
            action.queue(null, (throwable) ->
                    DiscordSRV.error("Failed to create permission override for network " + network.getChannel().getName() + ": " + throwable.getMessage())
            );
        } else {
            List<Permission> allowed;
            List<Permission> denied;
            if (isVoiceActivationAllowed()) {
                allowed = Arrays.asList(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD);
                denied = Collections.singletonList(Permission.VOICE_CONNECT);
            } else {
                allowed = Collections.singletonList(Permission.VOICE_SPEAK);
                denied = Arrays.asList(Permission.VOICE_CONNECT, Permission.VOICE_USE_VAD);
            }

            boolean dirty = false;
            PermOverrideManager manager = override.getManager();
            if (!override.getAllowed().containsAll(allowed)) {
                manager.grant(allowed);
                dirty = true;
            }
            if (!override.getDenied().containsAll(denied)) {
                manager.deny(denied);
                dirty = true;
            }
            if (dirty) manager.complete();
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerJoin(PlayerJoinEvent event) {
        dirtyPlayers.add(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerMove(PlayerMoveEvent event) {
        dirtyPlayers.add(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerTeleport(PlayerTeleportEvent event) {
        dirtyPlayers.add(event.getPlayer());
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onPlayerQuit(PlayerQuitEvent event) {
        networks.stream()
                .filter(network -> network.getPlayers().contains(event.getPlayer()))
                .forEach(network -> network.disconnect(event.getPlayer()));
    }

    @Override
    public void onGuildVoiceJoin(GuildVoiceJoinEvent event) {
        if (!event.getChannelJoined().equals(getLobbyChannel())) return;

        UUID uuid = DiscordSRV.getPlugin().getAccountLinkManager().getUuid(event.getMember().getUser().getId());
        if (uuid == null) return;
        OfflinePlayer player = Bukkit.getOfflinePlayer(uuid);
        if (player.isOnline()) dirtyPlayers.add(player.getPlayer());
    }

    public static Category getCategory() {
        return DiscordSRV.getPlugin().getJda().getCategoryById(DiscordSRV.config().getString("Voice category"));
    }

    public static VoiceChannel getLobbyChannel() {
        return DiscordSRV.getPlugin().getJda().getVoiceChannelById(DiscordSRV.config().getString("Lobby channel"));
    }

    public static Guild getGuild() {
        return getCategory().getGuild();
    }

    public static GuildController getGuildController() {
        return getCategory().getGuild().getController();
    }

    public static double getStrength() {
        return DiscordSRV.config().getDouble("Network.Strength");
    }

    public static double getFalloff() {
        return DiscordSRV.config().getDouble("Network.Falloff");
    }

    public static boolean isVoiceActivationAllowed() {
        return DiscordSRV.config().getBoolean("Network.Allow voice activation detection");
    }

    public static Member getMember(Player player) {
        String discordId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());
        return discordId != null ? getGuild().getMemberById(discordId) : null;
    }

    public Set<Network> getNetworks() {
        return networks;
    }

    public Set<Player> getDirtyPlayers() {
        return dirtyPlayers;
    }

}
