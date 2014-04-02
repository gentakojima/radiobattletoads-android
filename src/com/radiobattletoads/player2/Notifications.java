package com.radiobattletoads.player2;

import com.radiobattletoads.player2.DownloadCurrentinfo.DownloadCurrentInfoListener;
import com.radiobattletoads.player2.PlayerService.PlayerStatusChangeListener;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.preference.PreferenceManager;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

public class Notifications implements DownloadCurrentInfoListener, PlayerStatusChangeListener {

	private NotificationManager notificationManager = null;
	private Builder notifBuilder = null;
	private Context context;
	private boolean enabled = true;
	private boolean showNowPlayingNotification = false;

	public Notifications(Context context) {
		this.context = context;

		notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
		
		enabled = PreferenceManager.getDefaultSharedPreferences(this.context).getBoolean("notification", true);
	}
	
	public void onCreate() {
		DownloadCurrentinfo.register(this);
		PlayerService.register(this);
	}
	
	public void onDestroy() {
		DownloadCurrentinfo.unRegister(this);
		PlayerService.unRegister(this);
	}
	
	private  Builder getNotificationBuilder() {
		return new NotificationCompat.Builder(this.context)
		.setContentTitle("Radio Battletoads")
		.setSmallIcon(R.drawable.ic_launcher)
		.setAutoCancel(true).setOngoing(true);
	}

	public boolean addNotification() {
		
		// Check preference
		if(!enabled) return false;
		showNowPlayingNotification = true;

		// Build the intents
		Intent in = new Intent(this.context, PlayerActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this.context, 1, in, PendingIntent.FLAG_ONE_SHOT);

		Intent nextIntent = new Intent(this.context, PlayerService.class);
		nextIntent.setAction(PlayerService.ACTION_STOP);
		PendingIntent pauseIntent = PendingIntent.getService(this.context, 0, nextIntent, 0);

		// Build the notification
		Resources res = this.context.getResources();
		int notif_height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		int notif_width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

		notifBuilder = getNotificationBuilder();
		
		NowPlayingInfo npi = RBTPlayerApplication.getFromContext(context).getCachedNowPlayingInfo();
		if (npi == null) {
			notifBuilder.setContentIntent(contentIntent)
			.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent);
		} else {

			Bitmap large_bitmap = Bitmap.createScaledBitmap(npi.getCurrentArtwork(context), notif_width, notif_height, false);
			notifBuilder.setContentText(npi.toString())
					.setLargeIcon(large_bitmap)
					.setContentIntent(contentIntent)
					.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent);
		}
		Notification notifBuilt = notifBuilder.build();
		notifBuilt.flags = Notification.FLAG_NO_CLEAR;

		// Show the notification
		notificationManager.notify(0, notifBuilt);

		return true;
	}

	public boolean updateNotification() {

		if (!enabled || !showNowPlayingNotification) {
			return false;
		}
		
		NowPlayingInfo npi = RBTPlayerApplication.getFromContext(context).getCachedNowPlayingInfo();
		if (npi == null) {
			return false;
		}

		// Reuse previuos notification builder
		if (notifBuilder != null) {
			Resources res = this.context.getResources();
			int notif_height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
			int notif_width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
			Bitmap large_bitmap = Bitmap.createScaledBitmap(npi.getCurrentArtwork(this.context), notif_width, notif_height, false);
			notifBuilder.setContentText(npi.toString());
			notifBuilder.setLargeIcon(large_bitmap);
			Notification notifBuilt = notifBuilder.build();
			notifBuilt.flags = Notification.FLAG_NO_CLEAR;
			// Show the notification
			notificationManager.notify(0, notifBuilt);
			return true;
		} else {
			return false;
		}
	}

	public boolean removeNotification() {
		showNowPlayingNotification = false;
		DownloadCurrentinfo.unRegister(this);
		notificationManager.cancelAll();
		return true;
	}
	
	
	public boolean disable(){
		this.enabled = false;
		this.removeNotification();
		return true;
	}
	
	public boolean enable(){
		this.enabled = true;
		this.updateNotification();
		return true;
	}

	@Override
	public void onPlayingInformationChange(NowPlayingInfo newInfo) {
		this.updateNotification();
	}

	@Override
	public void onPlayingInformationDownloadError() {
		
	}

	@Override
	public void onPlayerUninitialized() {
		this.removeNotification();
	}

	@Override
	public void onPlayerReady() {
		
	}

	@Override
	public void onPlayerPlaying() {
		this.addNotification();
	}

	@Override
	public void onPlayerBuffering() {
		
	}

	@Override
	public void onPlayerInitializing() {
		
	}

	@Override
	public void onPlayerConnectionError() {
		
	}

	@Override
	public void onPlayerConnectionCut() {
		
	}

}
