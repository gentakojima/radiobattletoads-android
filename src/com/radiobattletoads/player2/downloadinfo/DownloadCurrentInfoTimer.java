package com.radiobattletoads.player2.downloadinfo;

import java.util.Timer;

import com.radiobattletoads.player2.DownloadCurrentinfo;

import android.content.Context;
import android.util.Log;

public class DownloadCurrentInfoTimer {

	private static final int UPDATE_INTERVAL_MILLIS = 18000;
	
	private Context context = null;
	private Timer downloadinfoTimer = null;
	private int startRequests = 0;
	
	public DownloadCurrentInfoTimer(Context context) {
		this.context = context;
	}
	
	public synchronized void start() {
		if (startRequests == 0) {
			doStart();
		}
		startRequests++;
	}
	
	public synchronized void stop() {
		startRequests--;
		if (startRequests == 0) {
			doStop();
		}
	}
	
	private void doStart() {
		Thread.dumpStack();
		doStop();
		downloadinfoTimer = new Timer();
		downloadinfoTimer.schedule(DownloadCurrentinfo.getTimerTask(this.context), 0, UPDATE_INTERVAL_MILLIS);
		Log.d("RBT", "DownloadInfoTimer: started");
	}
	
	private void doStop() {
		Thread.dumpStack();
		if (downloadinfoTimer != null) {
			downloadinfoTimer.cancel();
			downloadinfoTimer = null;
			Log.d("RBT", "DownloadInfoTimer: stopped");
		}
	}
	
}
