package com.scarsz.discordsrv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import net.dv8tion.jda.JDA;
import org.bukkit.plugin.Plugin;

@SuppressWarnings("unchecked")
public class ServerLogWatcher extends Thread{
	JDA api;
	Plugin plugin;
    public ServerLogWatcher(JDA api, Plugin plugin){
        this.api = api;
        this.plugin = plugin;
    }
    
	public void run(){
		int rate = plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate");
		String message = "";
		
		FileReader fr = null;
		try {
			fr = new FileReader(new File(new File(".").getAbsolutePath() + "/logs/latest.log").getAbsolutePath());
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
	    BufferedReader br = new BufferedReader(fr);
	    
	    Boolean done = false;
	    while (!done)
	    {
	    	String line = null;
			try {
				line = br.readLine();
			} catch (IOException e) {
				e.printStackTrace();
			}
			if (line == null) done = true;
	    }
	    
	    while (!isInterrupted())
	    {
	    	try {
			    if (DiscordSRV.consoleChannel == null) return;
		    	
		    	String line = null;
				try {
					line = br.readLine();
		    		DiscordSRV.DebugConsoleLogLinesProcessed++;
				} catch (IOException e) {
					e.printStackTrace();
				}
		    	if (line == null){
		    		DiscordSRV.DebugConsoleMessagesNull++;
		    		if (message.length() > 0){
		    			if (message.length() > 2000) message = message.substring(0, 1999);
						DiscordSRV.sendMessage(DiscordSRV.consoleChannel, message);
			    		DiscordSRV.DebugConsoleMessagesSent++;
						message = "";
					}
		    		try { Thread.sleep(rate); } catch (InterruptedException e) {}
		    		continue;
		    	}else{
		    		DiscordSRV.DebugConsoleMessagesNotNull++;
		    		for (String phrase : (List<String>) plugin.getConfig().getList("DiscordConsoleChannelDoNotSendPhrases")) if (line.toLowerCase().contains(phrase.toLowerCase())) continue;
		    		if (message.length() + line.length() + 2 <= 2000 && line.length() > 0){
		    			message += line + "\n";
		    		} else {
		    			DiscordSRV.sendMessage(DiscordSRV.consoleChannel, message);
			    		DiscordSRV.DebugConsoleMessagesSent++;
		    			message = line + "\n";
		    		}
		    	}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	}
}