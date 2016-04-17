package com.scarsz.discordsrv.threads;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;

import com.scarsz.discordsrv.DiscordSRV;

import net.dv8tion.jda.JDA;

public class ServerLogWatcher extends Thread {
	
	JDA api;
	
    public ServerLogWatcher(JDA api) {
        this.api = api;
    }

	public void run() {
		int rate = DiscordSRV.plugin.getConfig().getInt("DiscordConsoleChannelLogRefreshRate");
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
				} catch (IOException e) {
					e.printStackTrace();
				}
		    	if (line == null) {
		    		if (message.length() > 0) {
		    			if (message.length() > 2000) message = message.substring(0, 1999);
		    			sendMessage(message);
			    		message = "";
					}
		    		try { Thread.sleep(rate); } catch (InterruptedException e) {}
		    		continue;
		    	} else {
		    		for (String phrase : (List<String>) DiscordSRV.plugin.getConfig().getList("DiscordConsoleChannelDoNotSendPhrases")) if (line.toLowerCase().contains(phrase.toLowerCase())) continue;
		    		if (message.length() + line.length() + 2 <= 2000 && line.length() > 0) {
                        if (lineIsOk(applyRegex(line))) message += line + "\n";
		    		} else {
		    			sendMessage(message);
                        if (lineIsOk(applyRegex(line))) message = line + "\n";
		    		}
		    	}
	    	} catch (Exception e) {
	    		e.printStackTrace();
	    	}
	    }
	}

    private Boolean lineIsOk(String input) {
        return !input.replace(" ", "").replace("\n", "").isEmpty();
    }
	private void sendMessage(String input) {
		input = applyRegex(input);

		if (!input.replace(" ", "").replace("\n", "").isEmpty())
			DiscordSRV.sendMessage(DiscordSRV.consoleChannel, input);
	}
	private String applyRegex(String input) {
		return input.replaceAll(DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelRegexFilter"), DiscordSRV.plugin.getConfig().getString("DiscordConsoleChannelRegexReplacement"));
	}

}