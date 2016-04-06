package com.scarsz.discordsrv.threads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;

import com.scarsz.discordsrv.DiscordSRV;

import net.dv8tion.jda.JDA;

@SuppressWarnings("unchecked")
public class ServerLogWatcher extends Thread {
	
	JDA api;
	
    public ServerLogWatcher(JDA api) {
        this.api = api;
    }
    
	public void run() {
		int rate = DiscordSRV.plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate");
		int day = Calendar.DAY_OF_MONTH;
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
	    	// restart server log watcher thread if latest.log file rotates
	    	if (day != Calendar.DAY_OF_MONTH) new Thread(new Runnable() { @Override public void run() { DiscordSRV.startServerLogWatcher(); } }).start();
	    	
	    	try {
			    if (DiscordSRV.consoleChannel == null) return;
		    	
		    	String line = null;
				try {
					line = br.readLine();
		    		DiscordSRV.DebugConsoleLogLinesProcessed++;
				} catch (IOException e) {
					e.printStackTrace();
				}
		    	if (line == null) {
		    		DiscordSRV.DebugConsoleMessagesNull++;
		    		if (message.length() > 0) {
		    			if (message.length() > 2000) message = message.substring(0, 1999);
						DiscordSRV.sendMessage(DiscordSRV.consoleChannel, message);
			    		DiscordSRV.DebugConsoleMessagesSent++;
						message = "";
					}
		    		try { Thread.sleep(rate); } catch (InterruptedException e) {}
		    		continue;
		    	}else{
		    		DiscordSRV.DebugConsoleMessagesNotNull++;
		    		for (String phrase : (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordConsoleChannelDoNotSendPhrases")) if (line.toLowerCase().contains(phrase.toLowerCase())) continue;
		    		if (message.length() + line.length() + 2 <= 2000 && line.length() > 0) {
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