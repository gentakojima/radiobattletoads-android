package com.radiobattletoads.player2;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;

public class PlayerActivity extends Activity{
	
	public static PlayerActivity currentActivity = null;
	public static Context currentContext;
	
	public final static int MESSAGE_PLAYERSTATUS = 1; 

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

	}
	
	@SuppressLint("HandlerLeak")
	Handler messageHandler = new Handler(){public void handleMessage(Message m){
		
		TextView tv_status = (TextView)currentActivity.findViewById(R.id.playerStatus);
		
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
			}
			
			
			break;	
		}
		
	}};



}
