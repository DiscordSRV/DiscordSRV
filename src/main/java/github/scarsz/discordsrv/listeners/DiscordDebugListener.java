/*
 * DiscordSRV - A Minecraft to Discord and back link plugin
 * Copyright (C) 2016-2018 Austin "Scarsz" Shapiro
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

package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DebugUtil;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class DiscordDebugListener extends ListenerAdapter {

    private List<String> authorized = new ArrayList<String>() {{
        add("95088531931672576"); // Scarsz
        add("142968127829835777"); // Androkai
    }};

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // make sure it's not some random fucknut like mepeisen
        boolean authorizedDebugger =
                // user is a DiscordSRV developer
                authorized.contains(event.getAuthor().getId()) ||
                // user is in the AuthorizedDebuggers list
                DiscordSRV.config().getStringList("AuthorizedDebuggers").contains(event.getAuthor().getId()) ||
                // user is the owner of DiscordSRV's main guild
                (DiscordSRV.getPlugin().getMainGuild() != null && DiscordSRV.getPlugin().getMainGuild().getOwner().getUser().getId().equals(event.getAuthor().getId()));
        if (!authorizedDebugger) return;

        String message = event.getMessage().getContentRaw();

        if (!message.startsWith("*")) return;
        message = message.substring(1);

        if (message.matches("(discorddebug|discordsrvdebug).*")) {
            DiscordUtil.deleteMessage(event.getMessage());
            DiscordUtil.privateMessage(event.getAuthor(), DebugUtil.run(event.getAuthor().toString()));
        } else if (message.startsWith("eval ")) {
            if (!event.getGuild().getId().equals("135634590575493120") && !DiscordSRV.config().getStringList("EvalGuilds").contains(event.getGuild().getId())) {
                event.getMessage().addReaction("\uD83D\uDEAB").queue(); // 🚫
                return;
            }

            ScriptEngine scriptEngine = new ScriptEngineManager().getEngineByName("Nashorn");

            if (scriptEngine == null) {
                event.getMessage().addReaction("\u2753").queue(); // ❓
                return;
            }

            scriptEngine.put("event", event);
            scriptEngine.put("jda", event.getJDA());
            scriptEngine.put("guild", event.getGuild());
            scriptEngine.put("channel", event.getChannel());
            scriptEngine.put("server", Bukkit.getServer());
            scriptEngine.put("plugin", DiscordSRV.getPlugin());

            String statement = message.substring(5);
            long startTime = System.currentTimeMillis();

            String actualStatement = statement;
            for (Map.Entry<String, String> aliasEntry : new HashMap<String, String>() {{
                put("bukkit", "org.bukkit.Bukkit");
            }}.entrySet()) {
                actualStatement = "var " + aliasEntry.getKey() + " = " + aliasEntry.getValue() + ";" + statement;
            }

            try {
                String result = scriptEngine.eval(actualStatement).toString();
                event.getTextChannel().sendMessage("```java\n" + statement + "\n```\nEvaluated successfully in " + (System.currentTimeMillis() - startTime) + "ms:\n```java\n" + result + "\n```").queue();
            } catch (Exception e) {
                event.getTextChannel().sendMessage("```java\n" + statement + "\n```\nAn exception was thrown:\n```java\n"+e+"\n```").queue();
            }
        }
    }

}
