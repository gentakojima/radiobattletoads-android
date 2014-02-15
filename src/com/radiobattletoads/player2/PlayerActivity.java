package com.radiobattletoads.player2;


import java.util.Timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;

import android.support.v7.app.ActionBarActivity;

public class PlayerActivity extends ActionBarActivity{
	
	public static PlayerActivity currentActivity = null;
	public static Context currentContext;
	
	public final static int MESSAGE_PLAYERSTATUS = 1; 
	public final static int MESSAGE_CURRENTPROGRAM = 2;
	
	private Timer timer;
	
	public static final int STATUS_TRACKINFO_UNINITIALIZED=1;
	public static final int STATUS_TRACKINFO_INITIALIZED=2;
	public int status_trackinfo;

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
		
		// Add timer to download current track info and initialize
		status_trackinfo=STATUS_TRACKINFO_UNINITIALIZED;
	    timer = new Timer();
		timer.schedule(new DownloadCurrentinfo(),0, 15000);
		
		// Initialize player status
		LayoutParams trackInfoParams = new LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
				getWindowManager().getDefaultDisplay().getWidth() / 3 + 40);
		findViewById(R.id.trackInfoLayout).setLayoutParams(trackInfoParams);
		Message m = new Message();
		m.what = PlayerActivity.MESSAGE_PLAYERSTATUS;
		m.arg1 = PlayerService.status;
		PlayerActivity.currentActivity.messageHandler.sendMessage(m);

	}
	
	@SuppressLint("HandlerLeak")
	Handler messageHandler = new Handler(){@SuppressWarnings("deprecation")
	public void handleMessage(Message m){
			
		TextView tv_status = (TextView)currentActivity.findViewById(R.id.playerStatus);
		LinearLayout trackinfo_layout_container = (LinearLayout) currentActivity.findViewById(R.id.trackInfoLayout);
		final float scale = currentContext.getResources().getDisplayMetrics().density;
		
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
					
					LinearLayout trackinfo_layout = new LinearLayout(PlayerActivity.currentContext);
					LinearLayout trackinfo_textlayout = new LinearLayout(PlayerActivity.currentContext);
					ImageView trackinfo_image = new ImageView(PlayerActivity.currentContext);
					TextView trackinfo_title = new TextView(PlayerActivity.currentContext);
					TextView trackinfo_description = new TextView(PlayerActivity.currentContext);
					
					trackinfo_layout.addView(trackinfo_image);
					trackinfo_layout.addView(trackinfo_textlayout);
					
					LayoutParams trackinfo_image_params = new LayoutParams( getWindowManager().getDefaultDisplay().getWidth() / 3, 
							getWindowManager().getDefaultDisplay().getWidth() / 3, 1.0f);
					trackinfo_image_params.setMargins(20, 20, 7, 20);
					trackinfo_image.setLayoutParams(trackinfo_image_params);
					trackinfo_image.setScaleType(ScaleType.FIT_CENTER);
					
					LayoutParams trackinfo_textlayout_params = new LayoutParams( 2 * (getWindowManager().getDefaultDisplay().getWidth() / 3) , 
							LinearLayout.LayoutParams.MATCH_PARENT,1.0f);
					trackinfo_textlayout_params.setMargins(7, 20, 20, 20);
					trackinfo_textlayout.setLayoutParams(trackinfo_textlayout_params);
					
					trackinfo_textlayout.setOrientation(LinearLayout.VERTICAL);
					LayoutParams trackinfo_titledesc_params = new LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT, 
							LinearLayout.LayoutParams.MATCH_PARENT,1.0f);
					trackinfo_title.setLayoutParams(trackinfo_titledesc_params);
					trackinfo_textlayout.addView(trackinfo_title);
					trackinfo_description.setLayoutParams(trackinfo_titledesc_params);
					trackinfo_textlayout.addView(trackinfo_description);
					
					trackinfo_title.setTextSize((int) (12 * scale + 0.5f));
					trackinfo_title.setGravity(Gravity.BOTTOM);
					trackinfo_description.setTextSize((int) (9 * scale + 0.5f));
					trackinfo_description.setGravity(Gravity.TOP);
					trackinfo_description.setMaxLines(2);
					trackinfo_description.setEllipsize(TruncateAt.END);

					trackinfo_title.setText(m.getData().getString("title"));
					trackinfo_description.setText(m.getData().getString("description"));
					trackinfo_image.setImageBitmap((Bitmap) m.getData().getParcelable("artwork"));
					
					trackinfo_layout_container.removeAllViews();
					trackinfo_layout_container.addView(trackinfo_layout);
					
					break;
				case DownloadCurrentinfo.DOWNLOADCURRENTINFO_DOWNLOADING:
					
					break;
			}
			break;
		}
		
	}};



}
