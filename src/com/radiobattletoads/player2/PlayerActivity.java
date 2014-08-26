package com.radiobattletoads.player2;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
import android.text.TextUtils.TruncateAt;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.Animation.AnimationListener;
import android.view.animation.TranslateAnimation;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ImageView.ScaleType;
import android.widget.LinearLayout;
import android.widget.LinearLayout.LayoutParams;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.radiobattletoads.player2.DownloadCurrentinfo.DownloadCurrentInfoListener;
import com.radiobattletoads.player2.PlayerService.PlayerStatusChangeListener;

public class PlayerActivity extends ActionBarActivity implements DownloadCurrentInfoListener, PlayerStatusChangeListener,AnimationListener {

	public final static int MESSAGE_PLAYERSTATUS = 1;
	public final static int MESSAGE_CURRENTPROGRAM = 2;

	public static final int STATUS_TRACKINFO_UNINITIALIZED = 1;
	public static final int STATUS_TRACKINFO_INITIALIZED = 2;
	public int status_trackinfo;

	public static final String BROADCAST_PAUSE = "broadcast_pause";

	private RelativeLayout trackinfo_layout_container;
	private TextView tv_status;
	private LinearLayout bufferingLayout;
	private Button playButton;
	private Button pauseButton;
	private NowPlayingInfo currentDisplayedInfo = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Paint activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player_layout);
		PlayerService.register(this);

		// Add timer to download current track info and initialize
		status_trackinfo = STATUS_TRACKINFO_UNINITIALIZED;

		// Initialize player looks and status
		LayoutParams trackInfoParams;
		if( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ){
			trackInfoParams = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getHeight() / 3 + 40);
		}
		else{
			trackInfoParams = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getWidth() / 3 + 40);
		}
		findViewById(R.id.trackInfoLayout).setLayoutParams(trackInfoParams);
		
		// View cache
		trackinfo_layout_container = (RelativeLayout) findViewById(R.id.trackInfoLayout);
		tv_status = (TextView) findViewById(R.id.playerStatus);
		bufferingLayout = (LinearLayout) findViewById(R.id.bufferingLayout);
		playButton = (Button) findViewById(R.id.playbutton);
		pauseButton = (Button) findViewById(R.id.pauseButton);
		
		int status = PlayerService.getAbsoluteStatus();
		switch(status) {
		case PlayerService.PLAYER_UNINITIALIZED:
			this.onPlayerUninitialized();
			break;
		case PlayerService.PLAYER_BUFFERING:
			this.onPlayerBuffering();
			break;
		case PlayerService.PLAYER_CONNECTIONPROBLEM_CUT:
			this.onPlayerConnectionCut();
			break;
		case PlayerService.PLAYER_CONNECTIONPROBLEM_NOTSTARTED:
			this.onPlayerConnectionError();
			break;
		case PlayerService.PLAYER_INITIALIZING:
			this.onPlayerInitializing();
			break;
		case PlayerService.PLAYER_PLAYING:
			this.onPlayerPlaying();
			break;
		case PlayerService.PLAYER_READY:
			this.onPlayerReady();
			break;
		}
	}
	
	@Override
	public void onDestroy() {
		Log.d("RBT","Called onDestroy");
		super.onDestroy();
		PlayerService.unRegister(this);
		
	}
	
	@Override
	public void onResume() {
		super.onResume();
		
		// Show cached "now playing info"
		NowPlayingInfo npi = RBTPlayerApplication.getFromContext(this).getCachedNowPlayingInfo();
		if (npi != null && !npi.equals(currentDisplayedInfo)) {
			this.onPlayingInformationChange(npi);
		}
		DownloadCurrentinfo.register(this);
		// Start DownloadCurrentInfoTimer
		RBTPlayerApplication.getFromContext(this).getDownloadCurrentInfoTimer().start();
	}
	
	@Override
	public void onPause() {
		super.onPause();
		DownloadCurrentinfo.unRegister(this);
		// Stop DownloadCurrentInfoTimer
		RBTPlayerApplication.getFromContext(this).getDownloadCurrentInfoTimer().stop();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu items for use in the action bar
		MenuInflater inflater = getMenuInflater();
		inflater.inflate(R.menu.player_activity_actions, menu);
		return super.onCreateOptionsMenu(menu);
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		// Handle item selection
		switch (item.getItemId()) {
		case R.id.buttonslayout:
			/*
			 * Builder builder = new AlertDialog.Builder(this);
			 * builder.setTitle(R.string.about_title);
			 * builder.setMessage(R.string.about_body);
			 * builder.setNeutralButton(android.R.string.ok, null);
			 * builder.show();
			 */
			return true;
		case R.id.action_settings:
			Intent intent = new Intent(PlayerActivity.this, PreferencesActivity.class);
			PlayerActivity.this.startActivity(intent);
			return true;
		case R.id.action_about:
			Intent intent2 = new Intent(PlayerActivity.this, AboutActivity.class);
			PlayerActivity.this.startActivity(intent2);
			return true;
		default:
			return super.onOptionsItemSelected(item);
		}
	}

	public void onPlayPauseClick(View v) {

		Log.d("RBT", "Click event!");

		if (v.getId() == R.id.playbutton || v.getId() == R.id.pauseButton) {

			if (PlayerService.getInstance() == null) {
				// Initialize and play!
				Log.d("RBT", "Play");
				Intent intt = new Intent(this, PlayerService.class);
				intt.setAction(PlayerService.ACTION_START);
				this.startService(intt);
			} else {
				// Radio is playing, stop!
				Log.d("RBT", "Stop");
				this.stopService(new Intent(this, PlayerService.class));
			}

		}

	}

	@SuppressWarnings("deprecation")
	@Override
	public void onPlayingInformationChange(NowPlayingInfo newInfo) {
		if (this.isFinishing()) {
			return;
		}
		this.currentDisplayedInfo = newInfo;
		Log.d("RBT", "Received downloaded info");
		
		final float scale = getResources().getDisplayMetrics().density;

		LinearLayout old_trackinfo_layout = (LinearLayout) trackinfo_layout_container.getChildAt(0);
		LinearLayout trackinfo_layout = new LinearLayout(PlayerActivity.this.getApplicationContext());
		LinearLayout trackinfo_textlayout = new LinearLayout(PlayerActivity.this.getApplicationContext());
		ImageView trackinfo_image = new ImageView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_title = new TextView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_description = new TextView(PlayerActivity.this.getApplicationContext());
		ImageView background = (ImageView) findViewById(R.id.background);
		TextView playerstatus = (TextView) findViewById(R.id.playerStatus);
		LayoutParams playerstatus_params;
		LayoutParams trackinfo_image_params;
		LayoutParams trackinfo_params;
		LayoutParams trackinfo_textlayout_params;
		
		if( getResources().getConfiguration().orientation == Configuration.ORIENTATION_LANDSCAPE ){
			playerstatus_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 4.0f);
			trackinfo_image_params = new LayoutParams(getWindowManager().getDefaultDisplay().getHeight() / 3, LinearLayout.LayoutParams.MATCH_PARENT);
			trackinfo_textlayout_params = new LayoutParams(2 * (getWindowManager().getDefaultDisplay().getHeight() / 3), LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
		}
		else{
			playerstatus_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 2.0f);
			trackinfo_image_params = new LayoutParams(getWindowManager().getDefaultDisplay().getWidth() / 3, LinearLayout.LayoutParams.MATCH_PARENT);
			trackinfo_textlayout_params = new LayoutParams(2 * (getWindowManager().getDefaultDisplay().getWidth() / 3), LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
		}
		playerstatus.setLayoutParams(playerstatus_params);

		trackinfo_layout.addView(trackinfo_image);
		trackinfo_layout.addView(trackinfo_textlayout);

		trackinfo_image_params.setMargins(20, 20, 7, 20);
		trackinfo_image.setLayoutParams(trackinfo_image_params);
		trackinfo_image.setScaleType(ScaleType.FIT_CENTER);

		trackinfo_textlayout_params.setMargins(7, 20, 20, 20);
		trackinfo_textlayout.setLayoutParams(trackinfo_textlayout_params);

		trackinfo_textlayout.setOrientation(LinearLayout.VERTICAL);
		LayoutParams trackinfo_titledesc_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
		trackinfo_title.setLayoutParams(trackinfo_titledesc_params);
		trackinfo_textlayout.addView(trackinfo_title);
		trackinfo_description.setLayoutParams(trackinfo_titledesc_params);
		trackinfo_textlayout.addView(trackinfo_description);

		trackinfo_title.setTextSize((int) (12 * scale + 0.5f));
		trackinfo_title.setGravity(Gravity.BOTTOM);
		trackinfo_title.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
		trackinfo_title.setTextColor(Color.WHITE);
		trackinfo_description.setTextSize((int) (9 * scale + 0.5f));
		trackinfo_description.setGravity(Gravity.TOP);
		trackinfo_description.setMaxLines(2);
		trackinfo_description.setEllipsize(TruncateAt.END);
		trackinfo_description.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
		trackinfo_description.setTextColor(Color.WHITE);

		trackinfo_title.setText(newInfo.track_title);
		trackinfo_description.setText(newInfo.track_description);
		trackinfo_image.setImageBitmap(newInfo.artwork_image);
		
		/* Interchange background with a smooth animation */
		Drawable[] layers = new Drawable[2];
		try{
			layers[0] = new BitmapDrawable(getResources(), ((BitmapDrawable)background.getDrawable()).getBitmap());
		}
		catch(java.lang.ClassCastException e){
			layers[0] = new BitmapDrawable(getResources(), ((BitmapDrawable)((TransitionDrawable)background.getDrawable()).getDrawable(1)).getBitmap());
		}
		layers[1] = new BitmapDrawable(getResources(), newInfo.background_image);

		TransitionDrawable transitionDrawable = new TransitionDrawable(layers);
		background.setImageDrawable(transitionDrawable);
		transitionDrawable.startTransition(1000);
		
		TranslateAnimation translateAnimation1 = new TranslateAnimation(0f, getWindowManager().getDefaultDisplay().getWidth(), 0f, 0f);
		translateAnimation1.setDuration(700);
		translateAnimation1.setAnimationListener(this);
		TranslateAnimation translateAnimation2 = new TranslateAnimation(-getWindowManager().getDefaultDisplay().getWidth(), 0f, 0f, 0f);
		translateAnimation2.setDuration(700);
		old_trackinfo_layout.startAnimation(translateAnimation1);

		trackinfo_layout_container.addView(trackinfo_layout);
		trackinfo_layout.startAnimation(translateAnimation2);
		
	}

	@Override
	public void onPlayingInformationDownloadError() {
		if (this.isFinishing()) {
			return;
		}
		Log.d("RBT", "Not received downloaded info. Connection failed?");
		
		final float scale = getResources().getDisplayMetrics().density;
		
		LinearLayout trackinfoerror_layout = new LinearLayout(PlayerActivity.this.getApplicationContext());
		LayoutParams trackinfoerror_layout_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getWidth() / 3, 1.0f);
		trackinfoerror_layout.setLayoutParams(trackinfoerror_layout_params);

		LayoutParams trackinfoerror_textlayout_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT);
		trackinfoerror_textlayout_params.setMargins(20, 20, 20, 20);
		TextView trackinfoerror_text = new TextView(PlayerActivity.this.getApplicationContext());
		trackinfoerror_text.setTextSize((int) (12 * scale + 0.5f));
		trackinfoerror_text.setGravity(Gravity.CENTER);
		trackinfoerror_text.setText(R.string.cantdownloadinfo);
		trackinfoerror_text.setLayoutParams(trackinfoerror_textlayout_params);

		trackinfoerror_layout.addView(trackinfoerror_text);
		trackinfo_layout_container.removeViewAt(0);
		trackinfo_layout_container.addView(trackinfoerror_layout);
	}

	@Override
	public void onPlayerUninitialized() {
		tv_status.setText("Uninitialized");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerReady() {
		tv_status.setText("Ready (idle)");
		bufferingLayout.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerPlaying() {
		tv_status.setText("Playing");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPlayerBuffering() {
		tv_status.setText("Buffering...");
		bufferingLayout.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerInitializing() {
		tv_status.setText("Initializing...");
		bufferingLayout.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerConnectionError() {
		tv_status.setText("Check your connection!");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerConnectionCut() {
		tv_status.setText("The stream ended!");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}
	
	
	public void onConfigurationChanged (Configuration newConfig){
		
		super.onConfigurationChanged(newConfig);
		
		int orientation=newConfig.orientation;
		TextView playerstatus = (TextView) findViewById(R.id.playerStatus);
		LayoutParams playerstatus_params;

		switch(orientation) {

		case Configuration.ORIENTATION_LANDSCAPE:
			Log.d("RBT","Orientation changed to landscape");
			playerstatus_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 4.0f);
			playerstatus.setLayoutParams(playerstatus_params);
		 break;

		case Configuration.ORIENTATION_PORTRAIT:
			Log.d("RBT","Orientation changed to portrait");
			playerstatus_params = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.FILL_PARENT, 2.0f);
			playerstatus.setLayoutParams(playerstatus_params);
		 break;

		}
	}

	@Override
	public void onAnimationStart(Animation animation) {
		
	}

	@Override
	public void onAnimationEnd(Animation animation) {
		trackinfo_layout_container.removeViewAt(0);

	}

	@Override
	public void onAnimationRepeat(Animation animation) {

	}
	

}
