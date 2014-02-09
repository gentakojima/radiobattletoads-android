package com.radiobattletoads.player2;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.MediaList;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

public class PlayerService extends Service implements Runnable {
	
	public final static int PLAYER_UNINITIALIZED = 1;
	public final static int PLAYER_INITIALIZING = 2;
	public final static int PLAYER_READY = 3;
	public final static int PLAYER_BUFFERING = 4;
	public final static int PLAYER_PLAYING = 5;
	
	public static LibVLC mLibVLC = null;
	public static int status = PLAYER_UNINITIALIZED;
	
	public static PlayerService currentService;
	public static boolean sirThreadDiePleaseThankyou = false;
	public static Thread playerThread = null;
	
	protected void play(){
		// Play radio (assuming prepare was called before)
        mLibVLC.playIndex(0);
        mLibVLC.play();
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
        	mLibVLC.init(PlayerActivity.currentActivity);
        } catch(LibVlcException e) {
            Toast.makeText(PlayerActivity.currentActivity,
                    "Error initializing the libVLC multimedia framework!",
                    Toast.LENGTH_LONG).show();
            PlayerActivity.currentActivity.finish();
        }
        
        // Unset verbose mode
        mLibVLC.setVerboseMode(false);
        
        // Set 2 seconds for network caching
        mLibVLC.setNetworkCaching(2000);
        
        // Load media list, clear it and add radio
        MediaList list = mLibVLC.getPrimaryMediaList();
        list.clear();
        list.add(LibVLC.PathToURI("http://live.radiobattletoads.com:443/saltxero.ogg"));
        
        // Set current status
        setStatus(PLAYER_READY);
		
	}
	
	protected void setStatus(int s){
		PlayerService.status = s;
        if(PlayerActivity.currentActivity!=null){
			Message m = new Message();
			m.what = PlayerActivity.MESSAGE_PLAYERSTATUS;
			m.arg1 = PlayerService.status;
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
		// Prepare stream object and play
		prepare();
		play();
		while (playerThread!=null) {
            try {
            	// Change status?
            	if(mLibVLC.isPlaying() && PlayerService.status != PLAYER_PLAYING){
            		setStatus(PLAYER_PLAYING); 
            	}
            	Thread.sleep(500);
            } catch (InterruptedException e){
            }
        }
		stop();
		destroy();
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
