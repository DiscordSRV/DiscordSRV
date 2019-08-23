package github.scarsz.discordsrv.voice;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.core.Permission;
import net.dv8tion.jda.core.entities.Member;
import net.dv8tion.jda.core.entities.VoiceChannel;
import net.dv8tion.jda.core.events.channel.voice.VoiceChannelDeleteEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.entity.Player;

import java.util.*;

public class Network extends ListenerAdapter {

    private Set<Player> players = new HashSet<>();
    private String channel;

    public static Network with(Set<Player> players) {
        DiscordSRV.debug("Network being made for " + players);

        boolean allowVAD = VoiceModule.getConfig().dget("Network.Allow voice activation detection").as(Boolean.class);
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

    public boolean playerIsInRange(Player player) {
        return players.stream()
                .filter(p -> !p.equals(player))
                .anyMatch(p -> p.getLocation().distance(player.getLocation()) < VoiceModule.getInfluence());
    }

    public void connect(Player player) {
        players.add(player);

        Member member = VoiceModule.getMember(player);
        DiscordSRV.debug(player.getName() + "/" + member + " is connecting to " + getChannel());
        if (member != null && member.getVoiceState().inVoiceChannel()) {
            try {
                VoiceModule.getController().moveVoiceMember(member, getChannel()).complete();
            } catch (Exception e) {
                DiscordSRV.error("Failed to move member " + member + " into voice channel " + getChannel() + ": " + e.getMessage());
            }
        }
    }

    public void disconnect(Player player) {
        players.remove(player);
        if (players.size() <= 1) {
            die();
            return;
        }

        Member member = VoiceModule.getMember(player);
        DiscordSRV.debug(player.getName() + "/" + member + " is disconnecting from " + getChannel());
        if (member != null && member.getVoiceState().inVoiceChannel()) {
            try {
                VoiceModule.getController().moveVoiceMember(member, VoiceModule.getLobbyChannel()).complete();
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

        DiscordSRV.getPlugin().getVoiceModule().getNetworks().remove(this);
        DiscordSRV.getPlugin().getJda().removeEventListener(this);
        players.forEach(this::disconnect);
        if (getChannel() != null) {
            getChannel().getMembers().forEach(member -> {
                try {
                    VoiceModule.getController().moveVoiceMember(member, VoiceModule.getLobbyChannel()).complete();
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

    public Set<Player> getPlayers() {
        return players;
    }

}
