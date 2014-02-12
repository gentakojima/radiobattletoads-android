package com.radiobattletoads.player2;


import java.util.Timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.graphics.Bitmap;

public class PlayerActivity extends Activity{
	
	public static PlayerActivity currentActivity = null;
	public static Context currentContext;
	
	public final static int MESSAGE_PLAYERSTATUS = 1; 
	public final static int MESSAGE_CURRENTPROGRAM = 2;
	
	private Timer timer;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		
		// Update current activity and context
		currentActivity = this;
		currentContext = this.getApplicationContext();
		
		// Paint activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player_layout);
		
		// Set up listeners
		findViewById(R.id.playbutton).setOnClickListener(new ButtonManager());
		
		// Add timer to download current track info
	    timer = new Timer();
		timer.schedule(new DownloadCurrentinfo(),0, 60000);

	}
	
	@SuppressLint("HandlerLeak")
	Handler messageHandler = new Handler(){public void handleMessage(Message m){
		
		TextView tv_status = (TextView)currentActivity.findViewById(R.id.playerStatus);
		TextView tv_title = (TextView)currentActivity.findViewById(R.id.title);
    	TextView tv_description = (TextView)currentActivity.findViewById(R.id.description);
    	ImageView iv_artwork = (ImageView)currentActivity.findViewById(R.id.artwork);
		
		switch(m.what){
		case MESSAGE_PLAYERSTATUS:
			switch(m.arg1){
			case PlayerService.PLAYER_UNINITIALIZED:
				tv_status.setText("Uninitialized");				
				break;
			case PlayerService.PLAYER_READY:
				tv_status.setText("Ready (idle)");				
				break;
			case PlayerService.PLAYER_PLAYING:
				tv_status.setText("Playing");				
				break;
			case PlayerService.PLAYER_BUFFERING:
				tv_status.setText("Buffering...");				
				break;
			case PlayerService.PLAYER_INITIALIZING:
				tv_status.setText("Initializing...");				
				break;
			case PlayerService.PLAYER_CONNECTIONPROBLEM_NOTSTARTED:
				tv_status.setText("Check your connection!");				
				break;
			case PlayerService.PLAYER_CONNECTIONPROBLEM_CUT:
				tv_status.setText("The stream ended!");				
				break;
			}
			break;
		case MESSAGE_CURRENTPROGRAM:
			Log.d("RBT","Received info");
			switch(m.arg1){
				case DownloadCurrentinfo.DOWNLOADCURRENTINFO_UPDATED:
					Log.d("RBT","Received downloaded info");
					tv_title.setText(m.getData().getString("title"));
					tv_description.setText(m.getData().getString("description"));
					iv_artwork.setImageBitmap((Bitmap) m.getData().getParcelable("artwork"));
					break;
				case DownloadCurrentinfo.DOWNLOADCURRENTINFO_DOWNLOADING:
					
					break;
			}
			break;
		}
		
	}};



}
