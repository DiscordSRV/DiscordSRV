package github.scarsz.discordsrv.listeners;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.util.DebugUtil;
import github.scarsz.discordsrv.util.DiscordUtil;
import net.dv8tion.jda.core.entities.ChannelType;
import net.dv8tion.jda.core.events.message.MessageReceivedEvent;
import net.dv8tion.jda.core.hooks.ListenerAdapter;
import org.bukkit.Bukkit;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Made by Scarsz
 *
 * @in /dev/hell
 * @on 2/13/2017
 * @at 6:12 PM
 */
public class DiscordDebugListener extends ListenerAdapter {

    private List<String> authorized = new ArrayList<String>() {{
        add("95088531931672576"); // Scarsz
        add("142968127829835777"); // Androkai
    }};

    @Override
    public void onMessageReceived(MessageReceivedEvent event) {
        // make sure it's not some random fucknut like mepeisen
        if (!authorized.contains(event.getAuthor().getId()) && // one of the developers
                (event.isFromType(ChannelType.TEXT) && !event.getGuild().getOwner().getUser().getId().equals(event.getAuthor().getId())) // guild owner
        ) return;

        String message = event.getMessage().getRawContent();

        if (!message.startsWith("*")) return;
        message = message.substring(1);

        if (message.matches("discorddebug|discordsrvdebug")) {
            DiscordUtil.deleteMessage(event.getMessage());
            DiscordUtil.privateMessage(event.getAuthor(), DebugUtil.run(event.getAuthor().toString()));
        } else if (message.startsWith("eval ")) {
            ScriptEngine se = new ScriptEngineManager().getEngineByName("Nashorn");
            se.put("event", event);
            se.put("jda", event.getJDA());
            se.put("guild", event.getGuild());
            se.put("channel", event.getChannel());
            se.put("server", Bukkit.getServer());
            se.put("plugin", DiscordSRV.getPlugin());

            String statement = message.substring(5);
            long startTime = System.currentTimeMillis();

            String actualStatement = statement;
            for (Map.Entry<String, String> aliasEntry : new HashMap<String, String>() {{
                put("bukkit", "org.bukkit.Bukkit");
            }}.entrySet()) {
                actualStatement = "var " + aliasEntry.getKey() + " = " + aliasEntry.getValue() + ";" + statement;
            }

            try {
                String result = se.eval(actualStatement).toString();
                event.getTextChannel().sendMessage("```java\n" + statement + "\n```\nEvaluated successfully in " + (System.currentTimeMillis() - startTime) + "ms:\n```java\n" + result + "\n```").queue();
            } catch (Exception e) {
                event.getTextChannel().sendMessage("```java\n" + statement + "\n```\nAn exception was thrown:\n```java\n"+e+"\n```").queue();
            }
        }
    }

}
