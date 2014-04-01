package com.radiobattletoads.player2;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Timer;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.MediaList;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class PlayerService extends Service implements Runnable {

	// Static listeners
	private static ArrayList<PlayerStatusChangeListener> listeners = new ArrayList<PlayerStatusChangeListener>();
	public static void register(PlayerStatusChangeListener listener) {
		if (!listeners.contains(listener)) {
			listeners.add(listener);
		}
	}
	
	public static void unRegister(PlayerStatusChangeListener listener) {
		if (listeners.contains(listener)) {
			listeners.remove(listener);
		}
	}
	
	private static PlayerService instance = null;
	public static PlayerService getInstance() {
		return instance;
	}
	public static int getAbsoluteStatus() {
		if (instance == null) {
			return PLAYER_UNINITIALIZED;
		}
		return instance.status;
	}
	
	public final static int PLAYER_UNINITIALIZED = 1;
	public final static int PLAYER_INITIALIZING = 2;
	public final static int PLAYER_READY = 3;
	public final static int PLAYER_BUFFERING = 4;
	public final static int PLAYER_PLAYING = 5;
	public final static int PLAYER_CONNECTIONPROBLEM_NOTSTARTED = 11;
	public final static int PLAYER_CONNECTIONPROBLEM_CUT = 12;
	public final static String ACTION_START = "com.radiobattletoads.player2.PlayerService.start";
	public final static String ACTION_STOP = "com.radiobattletoads.player2.PlayerService.stop";

	private LibVLC mLibVLC = null;
	private int status = PLAYER_UNINITIALIZED;

	private boolean sirThreadDiePleaseThankyou = false;
	private Thread playerThread = null;
	private PlayerServiceHandler myHandler = new PlayerServiceHandler(this);
	private Timer downloadinfoTimer;

	protected void play() {
		// Play radio (assuming prepare was called before)
		mLibVLC.playIndex(0);
		// mLibVLC.play();
		// Set current status
		setStatusFromThread(PLAYER_BUFFERING);
	}

	protected void stop() {
		// Stop radio (assuming prepare was called before)
		mLibVLC.stop();
		// Set current status
		status = PLAYER_READY;
		// Send status to current activity
		setStatusFromThread(PLAYER_READY);
	}

	protected void destroy() {
		// Completely stop and destroy the multimedia framework
		mLibVLC.stop();
		mLibVLC.destroy();
		mLibVLC = null;
		// Set current status
		setStatusFromThread(PLAYER_UNINITIALIZED);
	}

	protected void prepare() {
		// Set current status
		setStatusFromThread(PLAYER_INITIALIZING);

		// Initialize the multimedia framework
		try {
			mLibVLC = LibVLC.getInstance();

			// Set network caching (defaults to 1,5s)
			SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
			mLibVLC.setNetworkCaching(Integer.parseInt(preferences.getString("buffering", "1500")));

			// Unset verbose mode
			mLibVLC.setVerboseMode(true);

			mLibVLC.init(this);
		} catch (LibVlcException e) {
			Log.d("RBT", "Error initializing the libVLC multimedia framework!");
			// TODO: Notify the user activity
		}

		// Load media list, clear it and add radio
		MediaList list = mLibVLC.getPrimaryMediaList();
		list.clear();
		list.add(LibVLC.PathToURI("http://live.radiobattletoads.com:443/saltxero.ogg"));

		// Set current status
		setStatusFromThread(PLAYER_READY);

	}

	protected void setStatus(int s) {

		// Transform transitional states to final states
		switch (s) {
		case PLAYER_CONNECTIONPROBLEM_CUT:
		case PLAYER_CONNECTIONPROBLEM_NOTSTARTED:
			status = PLAYER_UNINITIALIZED;
			break;
		default:
			status = s;
		}

		// Show or hide notification
		switch (status) {
		case PLAYER_PLAYING:
			if(downloadinfoTimer==null){
				downloadinfoTimer = new Timer();
				downloadinfoTimer.schedule(DownloadCurrentinfo.getTimerTask(this), 0, 18000);
			}
			RBTPlayerApplication.getFromContext(this).getNotifications().addNotification();
			break;
		case PLAYER_UNINITIALIZED:
			if(downloadinfoTimer!=null){
				downloadinfoTimer.cancel();
			}
			RBTPlayerApplication.getFromContext(this).getNotifications().removeNotification();
			break;
		}

		// Notify listeners
		for (int i = 0; i < PlayerService.listeners.size(); i++) {
			switch(status) {
			case PLAYER_UNINITIALIZED:
				PlayerService.listeners.get(i).onPlayerUninitialized();
				break;
			case PLAYER_READY:
				PlayerService.listeners.get(i).onPlayerReady();
				break;
			case PLAYER_BUFFERING:
				PlayerService.listeners.get(i).onPlayerBuffering();
				break;
			case PLAYER_CONNECTIONPROBLEM_CUT:
				PlayerService.listeners.get(i).onPlayerConnectionCut();
				break;
			case PLAYER_CONNECTIONPROBLEM_NOTSTARTED:
				PlayerService.listeners.get(i).onPlayerConnectionError();
				break;
			case PLAYER_INITIALIZING:
				PlayerService.listeners.get(i).onPlayerInitializing();
				break;
			case PLAYER_PLAYING:
				PlayerService.listeners.get(i).onPlayerPlaying();
				break;
			}
		}
		Log.d("RBT", "Status: " + s);
	}

	protected int getStatus() {
		return status;
	}
	
	private void setStatusFromThread(int status) {
		Message m = new Message();
		m.what = status;
		this.myHandler.sendMessage(m);
	}

	@Override
	public void run() {
		Log.d("RBT", "Thread is being run!");
		// Test connection
		if (NetworkStatus.getStatus(this) == NetworkStatus.NETWORK_DISCONNECTED) {
			this.stopSelf();
			super.onDestroy();
			playerThread = null;
			setStatusFromThread(PLAYER_CONNECTIONPROBLEM_NOTSTARTED);
			return;
		}

		int tryingToPlay = 0;
		boolean connectionCut = false;
		// Prepare stream object and play
		prepare();
		play();

		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
		Integer bufferingMsecs = Integer.parseInt(preferences.getString("buffering", "1500"));
		while (playerThread != null && !(status == PLAYER_PLAYING && !mLibVLC.isPlaying()) && tryingToPlay < (20 + (bufferingMsecs / 1000))) {
			try {
				// Change status?
				if (status != PLAYER_PLAYING) {
					if (mLibVLC.isPlaying()) {
						setStatusFromThread(PLAYER_PLAYING);
					} else {
						tryingToPlay++;
					}
				}
				Thread.sleep(500);
			} catch (InterruptedException e) {
			} catch (NullPointerException e) {
				 // Player is dead most probably by a pause button press, get out of here
				 return;
			}
		}
		if (status == PLAYER_PLAYING && !mLibVLC.isPlaying()) {
			connectionCut = true;
		}
		if (tryingToPlay >= 20) {
			// Too much tries, the connection didn't work!
			destroy();
			setStatusFromThread(PLAYER_CONNECTIONPROBLEM_NOTSTARTED);
		} else {
			if (connectionCut) {
				// The connection was probably shutdown!
				destroy();
				setStatusFromThread(PLAYER_CONNECTIONPROBLEM_CUT);
			} else {
				// Stopped normally
				stop();
				destroy();
			}
		}
	}

	@Override
	public int onStartCommand(Intent intent, int flags, int startId) {
		if (playerThread == null && ACTION_START.equals(intent.getAction())) {
			playerThread = new Thread(this);
			playerThread.start();
			Log.d("RBT", "Service started!");
			return START_STICKY;
		} else if (playerThread != null && ACTION_STOP.equals(intent.getAction())) {
			this.stopSelf();
		}
		return START_NOT_STICKY;
	}

	@Override
	public void onCreate() {
		/* We should only create one service */
		/* Berfen wrote: "Services are always singletons by nature!" */
		super.onCreate();
		PlayerService.instance = this;
		Log.d("RBT", "Service created!");
	}

	@Override
	public void onDestroy() {
		// this.stopSelf(); // This is redundant, I'm already dying. Let me die in peace.
		super.onDestroy();
		this.stop();
		this.destroy();
		PlayerService.instance = null;
		Log.d("RBT", "Service destroyed!");
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

	public static interface PlayerStatusChangeListener {
		void onPlayerUninitialized();

		void onPlayerReady();

		void onPlayerPlaying();

		void onPlayerBuffering();

		void onPlayerInitializing();

		void onPlayerConnectionError();

		void onPlayerConnectionCut();
	}
	
	static class PlayerServiceHandler extends Handler {
	    private final WeakReference<PlayerService> mService; 

	    public PlayerServiceHandler(PlayerService service) {
	        mService = new WeakReference<PlayerService>(service);
	    }
	    @Override
	    public void handleMessage(Message msg)
	    {
	    	PlayerService service = mService.get();
	         if (service != null) {
	              service.setStatus(msg.what);
	         }
	    }
	}

}
