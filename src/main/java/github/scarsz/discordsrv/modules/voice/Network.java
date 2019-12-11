package github.scarsz.discordsrv.modules.voice;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.VoiceChannel;
import net.dv8tion.jda.api.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.bukkit.entity.Player;

import java.util.*;

public class Network extends ListenerAdapter {

    private final Set<Player> players = new HashSet<>();
    private String channel;

    public static Network with(Set<Player> players) {
        DiscordSRV.debug("Network being made for " + players);

        boolean allowVAD = DiscordSRV.config().getBoolean("Network.Allow voice activation detection");
        List<Permission> allowedPermissions;
        if (allowVAD) {
            allowedPermissions = Arrays.asList(Permission.VOICE_SPEAK, Permission.VOICE_USE_VAD);
        } else {
            allowedPermissions = Collections.singletonList(Permission.VOICE_SPEAK);
        }

        VoiceChannel channel = (VoiceChannel) VoiceModule.getCategory().createVoiceChannel(UUID.randomUUID().toString())
                .addPermissionOverride(
                        VoiceModule.getGuild().getPublicRole(),
                        allowedPermissions,
                        Collections.singleton(Permission.VOICE_CONNECT)
                )
                .complete();

        return new Network(channel, players);
    }

    public Network(VoiceChannel channel, Set<Player> players) {
        this.channel = Objects.requireNonNull(channel).getId();
        players.forEach(this::connect);
        DiscordSRV.getPlugin().getJda().addEventListener(this);
    }

    public double getDistance(Player player) {
        return players.stream()
                .filter(p -> !p.equals(player))
                .filter(p -> p.getWorld().getName().equals(player.getWorld().getName()))
                .mapToDouble(p -> p.getLocation().distance(player.getLocation()))
                .min().orElse(Double.MAX_VALUE);
    }

    /**
     * @return true if the player is within the network strength or falloff ranges
     */
    public boolean playerIsInRange(Player player) {
        return getDistance(player) <= (VoiceModule.getStrength() + VoiceModule.getFalloff());
    }

    /**
     * @return true if the player is within the network strength and should be connected
     */
    public boolean playerIsInConnectionRange(Player player) {
        return getDistance(player) <= VoiceModule.getStrength();
    }

    /**
     * @return true if the player is within the falloff range <strong>but not the strength range</strong>
     */
    public boolean playerIsInFalloffRange(Player player) {
        double distance = getDistance(player);
        return distance >= VoiceModule.getStrength() &&
               distance <= (VoiceModule.getStrength() + VoiceModule.getFalloff());
    }

    public void connect(Player player) {
        players.add(player);

        Member member = VoiceModule.getMember(player);
        DiscordSRV.debug(player.getName() + "/" + member + " is connecting to " + getChannel());
        if (member != null && member.getVoiceState() != null && member.getVoiceState().inVoiceChannel()) {
            try {
                VoiceModule.getGuild().moveVoiceMember(member, getChannel()).complete();
            } catch (Exception e) {
                DiscordSRV.error("Failed to move member " + member + " into voice channel " + getChannel() + ": " + e.getMessage());
            }
        }
    }

    public void disconnect(Player player) {
        disconnect(player, true);
    }

    public void disconnect(Player player, boolean dieIfEmpty) {
        players.remove(player);
        if (players.size() <= 1 && dieIfEmpty) {
            die();
            return;
        }

        Member member = VoiceModule.getMember(player);
        DiscordSRV.debug(player.getName() + "/" + member + " is disconnecting from " + getChannel());
        if (member != null && member.getVoiceState().inVoiceChannel()) {
            try {
                VoiceModule.getGuild().moveVoiceMember(member, VoiceModule.getLobbyChannel()).complete();
            } catch (Exception e) {
                DiscordSRV.error("Failed to move member " + member + " into voice channel " + VoiceModule.getLobbyChannel() + ": " + e.getMessage());
            }
        }
    }

    public void engulf(Network network) {
        DiscordSRV.debug("Network " + this + " is engulfing " + network);
        network.players.forEach(this::connect);
        network.die();
    }

    public void die() {
        DiscordSRV.debug("Network " + this + " is dying");

        VoiceModule.get().getNetworks().remove(this);
        DiscordSRV.getPlugin().getJda().removeEventListener(this);
        new HashSet<>(players).forEach(player -> this.disconnect(player, false)); // new set made to prevent concurrent modification
        if (getChannel() != null) {
            getChannel().getMembers().forEach(member -> {
                try {
                    VoiceModule.getGuild().moveVoiceMember(member, VoiceModule.getLobbyChannel()).complete();
                } catch (Exception e) {
                    DiscordSRV.error("Failed to move member " + member + " into voice channel " + VoiceModule.getLobbyChannel() + ": " + e.getMessage());
                }
            });
            if (getChannel() != null) { // channel might be null by now
                getChannel()
                        .delete()
                        .reason("Lost communication")
                        .queue(null, null); // we don't care about if this passes or fails
            }
        }
    }

    @Override
    public void onVoiceChannelDelete(VoiceChannelDeleteEvent event) {
        if (event.getChannel().equals(getChannel())) {
            die();
        }
    }

    public VoiceChannel getChannel() {
        return DiscordSRV.getPlugin().getJda().getVoiceChannelById(channel);
    }

    public synchronized Set<Player> getPlayers() {
        return players;
    }

}
