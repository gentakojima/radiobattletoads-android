package com.radiobattletoads.player2;


import java.util.Date;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.TransitionDrawable;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
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
import android.widget.ScrollView;
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
	private LinearLayout moreinfo_layout;
	private LinearLayout bufferingLayout;
	private Button playButton;
	private Button pauseButton;
	private NowPlayingInfo currentDisplayedInfo = null;
	
	private Date currentDisplayedInfoTime = null;

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
		//tv_status = (TextView) findViewById(R.id.playerStatus);
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
		
		// Create timer to update time
		Thread t = new Thread() {

		  @Override
		  public void run() {
		    try {
		      while (!isInterrupted()) {
		        Thread.sleep(19000);
		        runOnUiThread(new Runnable() {
		          @Override
		          public void run() {
		            TextView tv = (TextView) findViewById(R.id.infotextTypedate);
		            if(tv!=null){
		            	Date ahora = new Date();
		            	Integer tiempopasado = (int) ((ahora.getTime() - PlayerActivity.this.currentDisplayedInfoTime.getTime())/1000);
		            	String newDate = null;
		            	if(PlayerActivity.this.currentDisplayedInfo.track_tipo.compareTo("continuidad")==0){
		            		newDate = PlayerActivity.this.timeToString(PlayerActivity.this.currentDisplayedInfo.track_empiezaen - tiempopasado);
		            	}
		            	else{
		            		newDate = PlayerActivity.this.timeToString(PlayerActivity.this.currentDisplayedInfo.track_empezadohace + tiempopasado);
		            	}
		            	tv.setText(tv.getText().toString().replaceAll("[0-9]+:[0-9]+$", newDate));
		            }
		          }
		        });
		      }
		    } catch (InterruptedException e) {
		    }
		  }
		};
		t.start();
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
		case R.id.buttonsLayout:
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
		TextView trackinfo_chapter = new TextView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_tipotiempo = new TextView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_twitter = new TextView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_web = new TextView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_desc = new TextView(PlayerActivity.this.getApplicationContext());
		ImageView background = (ImageView) findViewById(R.id.background);
		//TextView playerstatus = (TextView) findViewById(R.id.playerStatus);
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
		//playerstatus.setLayoutParams(playerstatus_params);

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
		trackinfo_chapter.setLayoutParams(trackinfo_titledesc_params);
		trackinfo_textlayout.addView(trackinfo_chapter);
		trackinfo_chapter.setLayoutParams(trackinfo_titledesc_params);
		trackinfo_textlayout.addView(trackinfo_tipotiempo);

		trackinfo_title.setTextSize((int) (11 * scale + 0.5f));
		trackinfo_title.setGravity(Gravity.CENTER_VERTICAL);
		trackinfo_title.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
		trackinfo_title.setTextColor(Color.WHITE);
		trackinfo_title.setMaxLines(1);
		trackinfo_title.setEllipsize(TruncateAt.END);
		trackinfo_title.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.2f));
		trackinfo_chapter.setTextSize((int) (9 * scale + 0.5f));
		trackinfo_chapter.setGravity(Gravity.CENTER_VERTICAL);
		trackinfo_chapter.setMaxLines(2);
		trackinfo_chapter.setEllipsize(TruncateAt.END);
		trackinfo_chapter.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
		trackinfo_chapter.setTextColor(Color.WHITE);
		trackinfo_chapter.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
		trackinfo_tipotiempo.setTextSize((int) (9 * scale + 0.5f));
		trackinfo_tipotiempo.setGravity(Gravity.CENTER_VERTICAL);
		trackinfo_tipotiempo.setMaxLines(2);
		trackinfo_tipotiempo.setEllipsize(TruncateAt.END);
		trackinfo_tipotiempo.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
		trackinfo_tipotiempo.setTextColor(Color.WHITE);
		trackinfo_tipotiempo.setLayoutParams(new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.MATCH_PARENT, 1.0f));
		
		// Set ID to update time
		trackinfo_tipotiempo.setId(R.id.infotextTypedate);
		currentDisplayedInfoTime = new Date();
		
		trackinfo_title.setText(newInfo.track_title);
		trackinfo_chapter.setText(newInfo.track_chapter);
		String tipotiempo;
		Log.d("RBT",newInfo.track_tipo);
		if(newInfo.track_tipo.compareTo("continuidad")==0){
			tipotiempo="Continuidad. Próximo programa en " + this.timeToString(newInfo.track_empiezaen);
		}
		else{
			if(newInfo.track_tipo.compareTo("directo")==0) tipotiempo="Directo. ";
			else if(newInfo.track_tipo.compareTo("estreno")==0) tipotiempo="Estreno. ";
			else if(newInfo.track_tipo.compareTo("reposicion")==0) tipotiempo="Reposición. ";
			else tipotiempo="Reposición automática. ";
			tipotiempo += "Empezó hace " + this.timeToString(newInfo.track_empezadohace);
		}
		trackinfo_tipotiempo.setText(tipotiempo);
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
		
		/* Info extra */
		this.moreinfo_layout = (LinearLayout) findViewById(R.id.moreinfo_layout);
		moreinfo_layout.removeAllViews();
		
		if(newInfo.track_desc!=null){
			trackinfo_desc.setText(newInfo.track_desc);
			trackinfo_desc.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
			trackinfo_desc.setTextColor(Color.WHITE);
			LayoutParams trackinfo_desc_params = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			trackinfo_desc_params.setMargins(20, 20, 20, 10);
			trackinfo_desc.setLayoutParams(trackinfo_desc_params);
			moreinfo_layout.addView(trackinfo_desc);
		}
		if(newInfo.track_web!=null){
			trackinfo_web.setText(newInfo.track_web.replaceAll("^https?://(www\\.)?", "").replaceAll("/$",""));
			trackinfo_web.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
			trackinfo_web.setTextColor(Color.WHITE);
			LayoutParams trackinfo_web_params = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			trackinfo_web_params.setMargins(20, 10, 20, 10);
			trackinfo_web.setLayoutParams(trackinfo_web_params);
			Drawable icon_link = getResources().getDrawable(R.drawable.icon_link);
			icon_link.setBounds(0, 0, 14, 14);
			trackinfo_web.setCompoundDrawablesRelative(icon_link, null, null, null);
			trackinfo_web.setCompoundDrawablePadding(10);
			moreinfo_layout.addView(trackinfo_web);
		}
		if(newInfo.track_twitter!=null){
			trackinfo_twitter.setText(newInfo.track_twitter);
			trackinfo_twitter.setShadowLayer((float) 4.0, 0, 0, Color.BLACK);
			trackinfo_twitter.setTextColor(Color.WHITE);
			LayoutParams trackinfo_twitter_params = new LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
			trackinfo_twitter_params.setMargins(20, 10, 20, 10);
			trackinfo_twitter.setLayoutParams(trackinfo_twitter_params);
			Drawable icon_twitter = getResources().getDrawable(R.drawable.icon_twitter);
			icon_twitter.setBounds(0, 0, 14, 12);
			trackinfo_twitter.setCompoundDrawablesRelative(icon_twitter, null, null, null);
			trackinfo_twitter.setCompoundDrawablePadding(10);
			moreinfo_layout.addView(trackinfo_twitter);
		}
		
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
		//tv_status.setText("Uninitialized");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerReady() {
		//tv_status.setText("Ready (idle)");
		bufferingLayout.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerPlaying() {
		//tv_status.setText("Playing");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.VISIBLE);
	}

	@Override
	public void onPlayerBuffering() {
		//tv_status.setText("Buffering...");
		bufferingLayout.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerInitializing() {
		//tv_status.setText("Initializing...");
		bufferingLayout.setVisibility(View.VISIBLE);
		playButton.setVisibility(View.GONE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerConnectionError() {
		//tv_status.setText("Check your connection!");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}

	@Override
	public void onPlayerConnectionCut() {
		//tv_status.setText("The stream ended!");
		bufferingLayout.setVisibility(View.GONE);
		playButton.setVisibility(View.VISIBLE);
		pauseButton.setVisibility(View.GONE);
	}
	
	
	public void onConfigurationChanged (Configuration newConfig){
		
		super.onConfigurationChanged(newConfig);
		
		int orientation=newConfig.orientation;
		RelativeLayout trackInfoLayout = (RelativeLayout) findViewById(R.id.trackInfoLayout);
		ScrollView scrollInfoLayout = (ScrollView) findViewById(R.id.scrollInfoLayout);
		RelativeLayout buttonsLayout = (RelativeLayout) findViewById(R.id.buttonsLayout);
		RelativeLayout rootLayout = (RelativeLayout) findViewById(R.id.rootlayout);

		switch(orientation) {

		case Configuration.ORIENTATION_LANDSCAPE:
			Log.d("RBT","Orientation changed to landscape");
			LinearLayout horizontalLayout = new LinearLayout(PlayerActivity.this.getApplicationContext());
			LinearLayout innerVerticalLayout = new LinearLayout(PlayerActivity.this.getApplicationContext());
			horizontalLayout.setOrientation(LinearLayout.HORIZONTAL);
			innerVerticalLayout.setOrientation(LinearLayout.VERTICAL);
			LinearLayout verticalLayout = (LinearLayout) findViewById(R.id.verticalLayout);
			verticalLayout.removeAllViews();
			horizontalLayout.setId(R.id.horizontalLayout);
			innerVerticalLayout.setId(R.id.innerVerticalLayout);
			innerVerticalLayout.addView(trackInfoLayout,0);
			innerVerticalLayout.addView(buttonsLayout,1);
			horizontalLayout.addView(innerVerticalLayout,0);
			horizontalLayout.addView(scrollInfoLayout,1);
			innerVerticalLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, 1.0f));
			scrollInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, 1.6f));
			rootLayout.removeViewAt(1);
			rootLayout.addView(horizontalLayout,1);
		 break;

		case Configuration.ORIENTATION_PORTRAIT:
			Log.d("RBT","Orientation changed to portrait");
			LinearLayout verticalLayout1 = new LinearLayout(PlayerActivity.this.getApplicationContext());
			verticalLayout1.setOrientation(LinearLayout.VERTICAL);
			verticalLayout1.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT));
			LinearLayout horizontalLayout1 = (LinearLayout) findViewById(R.id.horizontalLayout);
			LinearLayout innerVerticalLayout1 = (LinearLayout) findViewById(R.id.innerVerticalLayout);
			innerVerticalLayout1.removeAllViews();
			horizontalLayout1.removeAllViews();
			verticalLayout1.setId(R.id.verticalLayout);
			verticalLayout1.addView(trackInfoLayout,0);
			verticalLayout1.addView(scrollInfoLayout,1);
			verticalLayout1.addView(buttonsLayout,2);
			trackInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, 3.0f));
			scrollInfoLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, 2.0f));
			buttonsLayout.setLayoutParams(new LinearLayout.LayoutParams(
                    LayoutParams.MATCH_PARENT,
                    LayoutParams.MATCH_PARENT, 3.0f));
			rootLayout.removeViewAt(1);
			rootLayout.addView(verticalLayout1,1);
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
	
	@SuppressLint("DefaultLocale")
	private String timeToString(Integer t){
		if(t==null) return "";
		Integer t_mins = (int) Math.floor((t%3600)/60);
		Integer t_hours = (int) Math.floor(t/3600);
		return String.format("%02d:%02d", t_hours, t_mins);
		
	}
	

}
