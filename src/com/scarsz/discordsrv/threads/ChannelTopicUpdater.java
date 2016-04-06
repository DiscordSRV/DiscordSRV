package com.scarsz.discordsrv.threads;

import java.util.Date;
import java.util.concurrent.TimeUnit;

import org.bukkit.Bukkit;

import com.scarsz.discordsrv.DiscordSRV;

import net.dv8tion.jda.JDA;

public class ChannelTopicUpdater extends Thread {
	
	JDA api;
	
    public ChannelTopicUpdater(JDA api) {
        this.api = api;
    }
	
    public void run() {
    	int rate = DiscordSRV.plugin.getConfig().getInt("ChannelTopicUpdaterRateInSeconds") * 1000;
    	
	    while (!isInterrupted())
	    {
	    	try {
	    		String chatTopic = applyFormatters(DiscordSRV.plugin.getConfig().getString("ChannelTopicUpdaterChatChannelTopicFormat"));
	    		String consoleTopic = applyFormatters(DiscordSRV.plugin.getConfig().getString("ChannelTopicUpdaterConsoleChannelTopicFormat"));
	    		
	    		if (!chatTopic.isEmpty()) DiscordSRV.chatChannel.getManager().setTopic(chatTopic).update();
	    		if (!consoleTopic.isEmpty()) DiscordSRV.consoleChannel.getManager().setTopic(consoleTopic).update();
	    		
	    		Thread.sleep(rate);
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
    }
    
    private String applyFormatters(String input) {
    	input = input
    			.replace("%playercount%", Integer.toString(DiscordSRV.getOnlinePlayers().size()))
    			.replace("%playermax%", Integer.toString(Bukkit.getMaxPlayers()))
    			.replace("%date%", new Date().toString())
    			.replace("%totalplayers%", Integer.toString(Bukkit.getWorlds().get(0).getWorldFolder().listFiles().length))
    			.replace("%uptime%", Long.toString(TimeUnit.NANOSECONDS.toMinutes(System.nanoTime() - DiscordSRV.startTime)))
    			;
    	
    	return input;
    }
    
}