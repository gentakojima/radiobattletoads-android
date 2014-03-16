package com.radiobattletoads.player2;

import java.util.Random;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.provider.SyncStateContract.Constants;
import android.support.v4.app.NotificationCompat;

public class Notifications {
	
	static NotificationManager notificationManager = null;
	
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
		Notification n  = new NotificationCompat.Builder(PlayerActivity.currentContext)
		        .setContentTitle("Radio Battletoads")
		        .setContentText(DownloadCurrentinfo.getCurrentInfo())
		        .setLargeIcon(large_bitmap)
		        .setSmallIcon(R.drawable.ic_launcher)
		        .setContentIntent(contentIntent)
		        .setAutoCancel(true)
		        .setOngoing(true)
		        .addAction(android.R.drawable.ic_media_pause, "Pause", pauseIntent).build();
		n.flags = Notification.FLAG_NO_CLEAR;
		
		// Show the notification
		notificationManager.notify(0, n); 
		
		return true;
	}
	
	public static boolean removeNotification(){
		if(notificationManager==null){
			notificationManager = (NotificationManager)PlayerActivity.currentContext.getSystemService(PlayerActivity.currentContext.NOTIFICATION_SERVICE);
		}
		notificationManager.cancelAll();
		return true;
	}

}
