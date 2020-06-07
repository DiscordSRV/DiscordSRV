package github.scarsz.discordsrv.hooks.permissions;

import github.scarsz.discordsrv.DiscordSRV;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.entities.Role;
import org.bukkit.entity.Player;
import org.checkerframework.checker.nullness.qual.NonNull;

public class ContextCalculator implements net.luckperms.api.context.ContextCalculator<Player> {

    private static final String CONTEXT_LINKED = "discordsrv:linked";
    private static final String CONTEXT_BOOSTING = "discordsrv:boosting";
    private static final String CONTEXT_ROLE = "discordsrv:role";

    @Override
    public void calculate(@NonNull Player target, net.luckperms.api.context.ContextConsumer consumer) {
        String userId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(target.getUniqueId());
        consumer.accept(CONTEXT_LINKED, Boolean.toString(userId != null));

        if (userId == null) {
            return;
        }

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild == null) {
            return;
        }

        Member member = mainGuild.getMemberById(userId);
        if (member == null) {
            return;
        }

        consumer.accept(CONTEXT_BOOSTING, Boolean.toString(member.getTimeBoosted() != null));

        for (Role role : member.getRoles()) {
            consumer.accept(CONTEXT_ROLE, role.getName());
        }
    }

    @Override
    public net.luckperms.api.context.ContextSet estimatePotentialContexts() {
        net.luckperms.api.context.ImmutableContextSet.Builder builder = net.luckperms.api.context.ImmutableContextSet.builder();

        builder.add(CONTEXT_LINKED, "true");
        builder.add(CONTEXT_LINKED, "false");

        builder.add(CONTEXT_BOOSTING, "true");
        builder.add(CONTEXT_BOOSTING, "false");

        Guild mainGuild = DiscordSRV.getPlugin().getMainGuild();
        if (mainGuild != null) {
            for (Role role : mainGuild.getRoles()) {
                builder.add(CONTEXT_ROLE, role.getName());
            }
        }

        return builder.build();
    }

}
