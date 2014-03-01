package com.radiobattletoads.player2;


import java.util.Timer;

import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.TextView;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
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
		findViewById(R.id.pauseButton).setOnClickListener(new ButtonManager());
		
		// Add timer to download current track info and initialize
		status_trackinfo=STATUS_TRACKINFO_UNINITIALIZED;
	    timer = new Timer();
		timer.schedule(new DownloadCurrentinfo(),0, 15000);
		
		// Initialize player looks and status
		LayoutParams trackInfoParams = new LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
				getWindowManager().getDefaultDisplay().getWidth() / 3 + 40);
		findViewById(R.id.trackInfoLayout).setLayoutParams(trackInfoParams);
		Message m = new Message();
		m.what = PlayerActivity.MESSAGE_PLAYERSTATUS;
		m.arg1 = PlayerService.status;
		PlayerActivity.currentActivity.messageHandler.sendMessage(m);

	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
	    // Inflate the menu items for use in the action bar
	    MenuInflater inflater = getMenuInflater();
	    inflater.inflate(R.menu.player_activity_actions, menu);
	    return super.onCreateOptionsMenu(menu);
	}
	
	@SuppressLint("HandlerLeak")
	Handler messageHandler = new Handler(){@SuppressWarnings("deprecation")
	public void handleMessage(Message m){
			
		TextView tv_status = (TextView)currentActivity.findViewById(R.id.playerStatus);
		LinearLayout trackinfo_layout_container = (LinearLayout) currentActivity.findViewById(R.id.trackInfoLayout);
		final float scale = currentContext.getResources().getDisplayMetrics().density;
		LinearLayout bufferingLayout = (LinearLayout) currentActivity.findViewById(R.id.bufferingLayout);
		Button playButton = (Button) currentActivity.findViewById(R.id.playbutton);
		Button pauseButton = (Button) currentActivity.findViewById(R.id.pauseButton);
		
		switch(m.what){
		case MESSAGE_PLAYERSTATUS:
			switch(m.arg1){
			case PlayerService.PLAYER_UNINITIALIZED:
				tv_status.setText("Uninitialized");	
				bufferingLayout.setVisibility(View.GONE);
				playButton.setVisibility(View.VISIBLE);
				pauseButton.setVisibility(View.GONE);
				break;
			case PlayerService.PLAYER_READY:
				tv_status.setText("Ready (idle)");
				bufferingLayout.setVisibility(View.VISIBLE);
				playButton.setVisibility(View.GONE);
				pauseButton.setVisibility(View.GONE);
				break;
			case PlayerService.PLAYER_PLAYING:
				tv_status.setText("Playing");
				bufferingLayout.setVisibility(View.GONE);
				playButton.setVisibility(View.GONE);
				pauseButton.setVisibility(View.VISIBLE);
				break;
			case PlayerService.PLAYER_BUFFERING:
				tv_status.setText("Buffering...");
				bufferingLayout.setVisibility(View.VISIBLE);
				playButton.setVisibility(View.GONE);
				pauseButton.setVisibility(View.GONE);
				break;
			case PlayerService.PLAYER_INITIALIZING:
				tv_status.setText("Initializing...");
				bufferingLayout.setVisibility(View.VISIBLE);
				playButton.setVisibility(View.GONE);
				pauseButton.setVisibility(View.GONE);
				break;
			case PlayerService.PLAYER_CONNECTIONPROBLEM_NOTSTARTED:
				tv_status.setText("Check your connection!");	
				bufferingLayout.setVisibility(View.GONE);
				playButton.setVisibility(View.VISIBLE);
				pauseButton.setVisibility(View.GONE);
				break;
			case PlayerService.PLAYER_CONNECTIONPROBLEM_CUT:
				tv_status.setText("The stream ended!");
				bufferingLayout.setVisibility(View.GONE);
				playButton.setVisibility(View.VISIBLE);
				pauseButton.setVisibility(View.GONE);
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
					
					trackinfo_layout_container.removeViewAt(0);
					trackinfo_layout_container.addView(trackinfo_layout);
					
					break;
				case DownloadCurrentinfo.DOWNLOADCURRENTINFO_FAILED:
					Log.d("RBT","Not received downloaded info. Connection failed?");
					LinearLayout trackinfoerror_layout = new LinearLayout(PlayerActivity.currentContext);
					LayoutParams trackinfoerror_layout_params = new LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
							getWindowManager().getDefaultDisplay().getWidth() / 3, 1.0f);
					trackinfoerror_layout.setLayoutParams(trackinfoerror_layout_params);
					
					LayoutParams trackinfoerror_textlayout_params = new LayoutParams( LinearLayout.LayoutParams.MATCH_PARENT,
							LinearLayout.LayoutParams.MATCH_PARENT);
					trackinfoerror_textlayout_params.setMargins(20, 20, 20, 20);
					TextView trackinfoerror_text = new TextView(PlayerActivity.currentContext);
					trackinfoerror_text.setTextSize((int) (12 * scale + 0.5f));
					trackinfoerror_text.setGravity(Gravity.CENTER);
					trackinfoerror_text.setText(R.string.cantdownloadinfo);
					trackinfoerror_text.setLayoutParams(trackinfoerror_textlayout_params);
					
					trackinfoerror_layout.addView(trackinfoerror_text);
					trackinfo_layout_container.removeViewAt(0);
					trackinfo_layout_container.addView(trackinfoerror_layout);
					
					break;
				case DownloadCurrentinfo.DOWNLOADCURRENTINFO_DOWNLOADING:
					
					break;
			}
			break;
		}
		
	}};
	
	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
	    // Handle item selection
	    switch (item.getItemId()) {
	    case R.id.action_about:
	    	/*Builder builder = new AlertDialog.Builder(this);
	  	    builder.setTitle(R.string.about_title);
	  	    builder.setMessage(R.string.about_body);
	  	    builder.setNeutralButton(android.R.string.ok, null);
	  	    builder.show();*/
	        return true;
	    case R.id.action_settings:
	    	Intent intent = new Intent(PlayerActivity.this, PreferencesActivity.class);
	    	PlayerActivity.this.startActivity(intent);
	        return true;
	    default:
	        return super.onOptionsItemSelected(item);
	    }
	}
	



}
