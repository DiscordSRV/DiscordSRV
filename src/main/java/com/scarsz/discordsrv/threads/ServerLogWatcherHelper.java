package com.scarsz.discordsrv.threads;

import java.io.File;

import com.scarsz.discordsrv.DiscordSRV;

public class ServerLogWatcherHelper extends Thread {
	
	public void run() {
		double currentSize = new File(new File(new File(".").getAbsolutePath() + "/logs/latest.log").getAbsolutePath()).getTotalSpace();
		
		while (!interrupted()) {
			File logFile = new File(new File(new File(".").getAbsolutePath() + "/logs/latest.log").getAbsolutePath());
			
			if (logFile.getTotalSpace() < currentSize) DiscordSRV.startServerLogWatcher();
			else currentSize = logFile.getTotalSpace();
			
			try { Thread.sleep(15000); } catch (InterruptedException e) { e.printStackTrace(); }
		}
	}
	
}
