/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2020 Austin "Scarsz" Shapiro
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */

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
                synchronized (VoiceModule.getMutedUsers()) {
                    if (VoiceModule.getMutedUsers().contains(member.getId())) {
                        member.mute(false).queue();
                        VoiceModule.getMutedUsers().remove(member.getId());
                    }
                }
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
            VoiceModule.moveToLobby(member);
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
        VoiceChannel channel = getChannel();
        if (channel != null) {
            channel.getMembers().forEach(VoiceModule::moveToLobby);
            channel.delete().reason("Lost communication").queue(null, throwable ->
                    DiscordSRV.error("Failed to delete " + channel + ": " + throwable.getMessage())
            );
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
