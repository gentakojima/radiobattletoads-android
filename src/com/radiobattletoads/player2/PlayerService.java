package com.radiobattletoads.player2;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.MediaList;

import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.IBinder;
import android.os.Message;
import android.preference.PreferenceManager;
import android.util.Log;

public class PlayerService extends Service implements Runnable {
	
	public final static int PLAYER_UNINITIALIZED = 1;
	public final static int PLAYER_INITIALIZING = 2;
	public final static int PLAYER_READY = 3;
	public final static int PLAYER_BUFFERING = 4;
	public final static int PLAYER_PLAYING = 5;
	public final static int PLAYER_CONNECTIONPROBLEM_NOTSTARTED = 11;
	public final static int PLAYER_CONNECTIONPROBLEM_CUT = 12;
	
	public static LibVLC mLibVLC = null;
	public static int status = PLAYER_UNINITIALIZED;
	
	public static PlayerService currentService;
	public static boolean sirThreadDiePleaseThankyou = false;
	public static Thread playerThread = null;
	
	protected void play(){
		// Play radio (assuming prepare was called before)
        mLibVLC.playIndex(0);
        //mLibVLC.play();
        // Set current status
        setStatus(PLAYER_BUFFERING);
	}
	
	protected void stop(){
		// Stop radio (assuming prepare was called before)
		mLibVLC.stop();
		// Set current status
		PlayerService.status = PLAYER_READY;
		// Send status to current activity
		setStatus(PLAYER_READY);
	}
	
	protected void destroy(){
		// Completely stop and destroy the multimedia framework
		mLibVLC.stop();
		mLibVLC.destroy();
		mLibVLC = null;
		// Set current status
		setStatus(PLAYER_UNINITIALIZED);
	}
	
	protected void prepare(){
		// Set current status
		setStatus(PLAYER_INITIALIZING);
		
		// Initialize the multimedia framework
        try {
        	mLibVLC = LibVLC.getInstance();
        	
        	// Set network caching (defaults to 1,5s)
        	SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PlayerActivity.currentActivity);
            mLibVLC.setNetworkCaching(Integer.parseInt(preferences.getString("buffering", "1500")));
        	
        	// Unset verbose mode
            mLibVLC.setVerboseMode(true);
            
        	mLibVLC.init(PlayerActivity.currentActivity);
        } catch(LibVlcException e) {
            Log.d("RBT","Error initializing the libVLC multimedia framework!");
            PlayerActivity.currentActivity.finish();
        }

        // Load media list, clear it and add radio
        MediaList list = mLibVLC.getPrimaryMediaList();
        list.clear();
        list.add(LibVLC.PathToURI("http://live.radiobattletoads.com:443/saltxero.ogg"));
        
        // Set current status
        setStatus(PLAYER_READY);
		
	}
	
	protected void setStatus(int s){
		switch(s){
		case PLAYER_CONNECTIONPROBLEM_CUT:
		case PLAYER_CONNECTIONPROBLEM_NOTSTARTED:
			PlayerService.status = PLAYER_UNINITIALIZED;
			break;
			default:
				PlayerService.status = s;
		}
		
        if(PlayerActivity.currentActivity!=null){
			Message m = new Message();
			m.what = PlayerActivity.MESSAGE_PLAYERSTATUS;
			m.arg1 = s;
			PlayerActivity.currentActivity.messageHandler.sendMessage(m);
		}
        Log.d("RBT","Status: " + s);
	}
	
	protected int getStatus(){
		return PlayerService.status;
	}

	@Override
	public void run() {
		Log.d("RBT","Thread is being run!");
		// Test connection
		if(NetworkStatus.getStatus(PlayerActivity.currentContext)==NetworkStatus.NETWORK_DISCONNECTED){
			this.stopSelf();
			super.onDestroy();
			playerThread=null;
			setStatus(PLAYER_CONNECTIONPROBLEM_NOTSTARTED);
			return;
		}
		
		int tryingToPlay=0;
		boolean connectionCut = false;
		// Prepare stream object and play
		prepare();
		play();
		
		SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(PlayerActivity.currentActivity);
		Integer bufferingMsecs = Integer.parseInt(preferences.getString("buffering", "1500"));
		while (playerThread!=null && !(status==PLAYER_PLAYING && !mLibVLC.isPlaying()) && tryingToPlay<(20+(bufferingMsecs/1000))) {
            try {
            	// Change status?
            	if(PlayerService.status != PLAYER_PLAYING){
            		if(mLibVLC.isPlaying()){
            			setStatus(PLAYER_PLAYING);
            		}
            		else{
            			tryingToPlay++;
            		}
            	}
            	Thread.sleep(500);
            } catch (InterruptedException e){
            }
        }
		if(status==PLAYER_PLAYING && !mLibVLC.isPlaying()){
			connectionCut = true;
		}
		if(tryingToPlay>=20){
			// Too much tries, the connection didn't work!
			destroy();
			setStatus(PLAYER_CONNECTIONPROBLEM_NOTSTARTED);
		}
		else{
			if(connectionCut){
				// The connection was probably shutdown!
				destroy();
				setStatus(PLAYER_CONNECTIONPROBLEM_CUT);
			}
			else{
				// Stopped normally
				stop();
				destroy();
			}
		}
	}

	@Override
    public int onStartCommand (Intent intent, int flags, int startId){
        playerThread = new Thread(this);
		playerThread.start();
		Log.d("RBT","Service started!");
		return START_STICKY;
	}
	
	@Override
    public void onCreate() {
		/* We should only create one service */
		if(currentService!=null){
			return;
		}
        super.onCreate();
        Log.d("RBT","Service created!");
	}
	
	@Override
    public void onDestroy() {
		this.stopSelf();
		super.onDestroy();
		playerThread = null;
		Log.d("RBT","Service destroyed!");
	}

	@Override
	public IBinder onBind(Intent intent) {
		// TODO Auto-generated method stub
		return null;
	}

}
