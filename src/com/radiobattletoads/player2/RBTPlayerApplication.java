package com.radiobattletoads.player2;

import com.radiobattletoads.player2.downloadinfo.DownloadCurrentInfoTimer;

import android.app.Application;
import android.content.Context;

public class RBTPlayerApplication extends Application {

	private Notifications notifications = null;
	private NowPlayingInfo cachedNowPlayingInfo = null;
	private DownloadCurrentInfoTimer downloadInfoTimer = null;

	@Override
	public void onCreate() {
		super.onCreate();
		this.notifications = new Notifications(this);
		this.notifications.onCreate();
		
		this.downloadInfoTimer = new DownloadCurrentInfoTimer(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
		this.notifications.onDestroy();
	}

	public Notifications getNotifications() {
		return this.notifications;
	}
	
	public NowPlayingInfo getCachedNowPlayingInfo() {
		return this.cachedNowPlayingInfo;
	}
	
	public void setCachedNowPlayingInfo(NowPlayingInfo cachedInfo) {
		this.cachedNowPlayingInfo = cachedInfo;
	}
	
	public DownloadCurrentInfoTimer getDownloadCurrentInfoTimer() {
		return this.downloadInfoTimer;
	}

	public static RBTPlayerApplication getFromContext(Context context) {
		return (RBTPlayerApplication) context.getApplicationContext();
	}

}
