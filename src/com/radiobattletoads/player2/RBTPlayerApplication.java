package com.radiobattletoads.player2;

import android.app.Application;
import android.content.Context;

public class RBTPlayerApplication extends Application {

	private Notifications notifications = null;
	private NowPlayingInfo cachedNowPlayingInfo = null;

	@Override
	public void onCreate() {
		super.onCreate();
		this.notifications = new Notifications(this);
	}

	@Override
	public void onTerminate() {
		super.onTerminate();
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

	public static RBTPlayerApplication getFromContext(Context context) {
		return (RBTPlayerApplication) context.getApplicationContext();
	}

}
