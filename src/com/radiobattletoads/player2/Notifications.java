package com.radiobattletoads.player2;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationCompat.Builder;

public class Notifications {
	
	static NotificationManager notificationManager = null;
	static Builder notif = null;
	
	public static boolean addNotification(){
		
		if(notificationManager==null){
			notificationManager = (NotificationManager)PlayerActivity.currentContext.getSystemService(PlayerActivity.currentContext.NOTIFICATION_SERVICE);
		}
		
		// Build the intents
		Intent in = new Intent(PlayerActivity.currentContext, PlayerActivity.class);
        PendingIntent contentIntent = PendingIntent.getActivity(PlayerActivity.currentContext, 1, in, PendingIntent.FLAG_ONE_SHOT);
        
        Intent nextIntent = new Intent(PlayerActivity.BROADCAST_PAUSE);
        PendingIntent pauseIntent = PendingIntent.getBroadcast(PlayerActivity.currentContext, 0, nextIntent, 0);

		// Build the notification
        Resources res = PlayerActivity.currentContext.getResources();
        int notif_height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
        int notif_width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
        Bitmap large_bitmap = Bitmap.createScaledBitmap(DownloadCurrentinfo.getCurrentArtwork(),notif_width,notif_height,false);
		notif  = new NotificationCompat.Builder(PlayerActivity.currentContext)
		        .setContentTitle("Radio Battletoads")
		        .setContentText(DownloadCurrentinfo.getCurrentInfo())
		        .setLargeIcon(large_bitmap)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(contentIntent)
		        .setAutoCancel(true)
		        .setOngoing(true)
		        .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent);
		Notification notifBuilt = notif.build();
		notifBuilt.flags = Notification.FLAG_NO_CLEAR;
		
		// Show the notification
		notificationManager.notify(0, notifBuilt); 
		
		return true;
	}
	
	public static boolean updateNotification(){
		
		if(notificationManager==null){
			notificationManager = (NotificationManager)PlayerActivity.currentContext.getSystemService(PlayerActivity.currentContext.NOTIFICATION_SERVICE);
		}
		
		if(notif!=null){
			Resources res = PlayerActivity.currentContext.getResources();
			int notif_height = (int) res.getDimension(android.R.dimen.notification_large_icon_height);
	        int notif_width = (int) res.getDimension(android.R.dimen.notification_large_icon_width);
	        Bitmap large_bitmap = Bitmap.createScaledBitmap(DownloadCurrentinfo.getCurrentArtwork(),notif_width,notif_height,false);
			notif.setContentText(DownloadCurrentinfo.getCurrentInfo());
			notif.setLargeIcon(large_bitmap);
			Notification notifBuilt = notif.build();
			notifBuilt.flags = Notification.FLAG_NO_CLEAR;
			// Show the notification
			notificationManager.notify(0, notifBuilt); 
			return true;
		}
		else{
			return false;
		}
	}
	
	public static boolean removeNotification(){
		if(notificationManager==null){
			notificationManager = (NotificationManager)PlayerActivity.currentContext.getSystemService(PlayerActivity.currentContext.NOTIFICATION_SERVICE);
		}
		notificationManager.cancelAll();
		return true;
	}

}
