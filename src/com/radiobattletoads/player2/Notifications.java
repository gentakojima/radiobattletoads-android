package com.radiobattletoads.player2;

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

public class Notifications {

	private NotificationManager notificationManager = null;
	private Builder notif = null;
	private Context context;

	public Notifications(Context context) {
		this.context = context;

		notificationManager = (NotificationManager) this.context.getSystemService(Context.NOTIFICATION_SERVICE);
	}

	public boolean addNotification() {
		
		// Check preference
		Boolean enabled = PreferenceManager.getDefaultSharedPreferences(this.context).getBoolean("notification", true);
		if(!enabled) return false;		

		// Build the intents
		Intent in = new Intent(this.context, PlayerActivity.class);
		PendingIntent contentIntent = PendingIntent.getActivity(this.context, 1, in, PendingIntent.FLAG_ONE_SHOT);

		Intent nextIntent = new Intent(PlayerActivity.BROADCAST_PAUSE);
		PendingIntent pauseIntent = PendingIntent.getBroadcast(this.context, 0, nextIntent, 0);

		// Build the notification
		Resources res = this.context.getResources();
		int notif_height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
		int notif_width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);

		NowPlayingInfo npi = RBTPlayerApplication.getFromContext(context).getCachedNowPlayingInfo();
		if (npi == null) {
			return false;
		}

		Bitmap large_bitmap = Bitmap.createScaledBitmap(npi.getCurrentArtwork(context), notif_width, notif_height, false);
		notif = new NotificationCompat.Builder(this.context)
				.setContentTitle("Radio Battletoads")
				.setContentText(npi.toString())
				.setLargeIcon(large_bitmap)
				.setSmallIcon(R.drawable.ic_launcher)
				.setContentIntent(contentIntent)
				.setAutoCancel(true).setOngoing(true)
				.addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent);
		Notification notifBuilt = notif.build();
		notifBuilt.flags = Notification.FLAG_NO_CLEAR;

		// Show the notification
		notificationManager.notify(0, notifBuilt);

		return true;
	}

	public boolean updateNotification() {

		NowPlayingInfo npi = RBTPlayerApplication.getFromContext(context).getCachedNowPlayingInfo();
		if (npi == null) {
			return false;
		}

		if (notif != null) {
			Resources res = this.context.getResources();
			int notif_height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
			int notif_width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
			Bitmap large_bitmap = Bitmap.createScaledBitmap(npi.getCurrentArtwork(this.context), notif_width, notif_height, false);
			notif.setContentText(npi.toString());
			notif.setLargeIcon(large_bitmap);
			Notification notifBuilt = notif.build();
			notifBuilt.flags = Notification.FLAG_NO_CLEAR;
			// Show the notification
			notificationManager.notify(0, notifBuilt);
			return true;
		} else {
			return false;
		}
	}

	public boolean removeNotification() {
		notificationManager.cancelAll();
		return true;
	}
	
	
	public boolean disable(){
		this.removeNotification();
		return true;
	}
	
	public boolean enable(){
		
		return true;
	}

}
