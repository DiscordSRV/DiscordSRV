package com.scarsz.discordsrv;

import org.bukkit.plugin.Plugin;

import net.dv8tion.jda.JDA;

public class ChannelTopicUpdater extends Thread {
	
	JDA api;
	Plugin plugin;
	
    public ChannelTopicUpdater(JDA api, Plugin plugin) {
        this.api = api;
        this.plugin = plugin;
    }
	
    public void run() {
    	int rate = plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate");
    	
	    while (!isInterrupted())
	    {
	    	try {
	    		String chatTopic = "";
	    		String consoleTopic = "";
	    		
	    		DiscordSRV.chatChannel.getManager().setTopic("").update();
	    		DiscordSRV.consoleChannel.getManager().setTopic("").update();
	    		Thread.sleep(rate);
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
    }
    
}