package com.radiobattletoads.player2;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.ActionBarActivity;
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

import com.radiobattletoads.player2.DownloadCurrentinfo.DownloadCurrentInfoListener;
import com.radiobattletoads.player2.PlayerService.PlayerStatusChangeListener;

public class PlayerActivity extends ActionBarActivity implements DownloadCurrentInfoListener, PlayerStatusChangeListener {

	public final static int MESSAGE_PLAYERSTATUS = 1;
	public final static int MESSAGE_CURRENTPROGRAM = 2;

	public static final int STATUS_TRACKINFO_UNINITIALIZED = 1;
	public static final int STATUS_TRACKINFO_INITIALIZED = 2;
	public int status_trackinfo;

	public static final String BROADCAST_PAUSE = "broadcast_pause";

	private LinearLayout trackinfo_layout_container;
	private TextView tv_status;
	private LinearLayout bufferingLayout;
	private Button playButton;
	private Button pauseButton;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		// Paint activity
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player_layout);
		PlayerService.register(this);
		DownloadCurrentinfo.register(this);

		// Add timer to download current track info and initialize
		status_trackinfo = STATUS_TRACKINFO_UNINITIALIZED;

		// Initialize player looks and status
		LayoutParams trackInfoParams = new LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, getWindowManager().getDefaultDisplay().getWidth() / 3 + 40);
		findViewById(R.id.trackInfoLayout).setLayoutParams(trackInfoParams);
		
		// View cache
		trackinfo_layout_container = (LinearLayout) findViewById(R.id.trackInfoLayout);
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

		// Launch first DownloadCurrentInfo Task
		DownloadCurrentinfo.getTimerTask(this).run();
	}
	
	@Override
	public void onDestroy() {
		Log.d("RBT","Called onDestroy");
		super.onDestroy();
		PlayerService.unRegister(this);
		DownloadCurrentinfo.unRegister(this);
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
		case R.id.action_about:
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

	@Override
	public void onPlayingInformationChange(NowPlayingInfo newInfo) {
		if (this.isFinishing()) {
			return;
		}
		Log.d("RBT", "Received downloaded info");
		
		final float scale = getResources().getDisplayMetrics().density;

		LinearLayout trackinfo_layout = new LinearLayout(PlayerActivity.this.getApplicationContext());
		LinearLayout trackinfo_textlayout = new LinearLayout(PlayerActivity.this.getApplicationContext());
		ImageView trackinfo_image = new ImageView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_title = new TextView(PlayerActivity.this.getApplicationContext());
		TextView trackinfo_description = new TextView(PlayerActivity.this.getApplicationContext());

		trackinfo_layout.addView(trackinfo_image);
		trackinfo_layout.addView(trackinfo_textlayout);

		LayoutParams trackinfo_image_params = new LayoutParams(getWindowManager().getDefaultDisplay().getWidth() / 3, getWindowManager().getDefaultDisplay().getWidth() / 3, 1.0f);
		trackinfo_image_params.setMargins(20, 20, 7, 20);
		trackinfo_image.setLayoutParams(trackinfo_image_params);
		trackinfo_image.setScaleType(ScaleType.FIT_CENTER);

		LayoutParams trackinfo_textlayout_params = new LayoutParams(2 * (getWindowManager().getDefaultDisplay().getWidth() / 3), LinearLayout.LayoutParams.MATCH_PARENT, 1.0f);
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
		trackinfo_description.setTextSize((int) (9 * scale + 0.5f));
		trackinfo_description.setGravity(Gravity.TOP);
		trackinfo_description.setMaxLines(2);
		trackinfo_description.setEllipsize(TruncateAt.END);

		trackinfo_title.setText(newInfo.track_title);
		trackinfo_description.setText(newInfo.track_description);
		trackinfo_image.setImageBitmap(newInfo.artwork_image);

		trackinfo_layout_container.removeViewAt(0);
		trackinfo_layout_container.addView(trackinfo_layout);
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

}
