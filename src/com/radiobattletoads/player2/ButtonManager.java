package com.radiobattletoads.player2;

import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.LibVlcException;
import org.videolan.libvlc.MediaList;

import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.TextView;
import android.widget.Toast;
public class ButtonManager implements OnClickListener {

	public void onClick(View v) {
		
		Log.d("RBT","Click event!");
		
		if(PlayerActivity.mLibVLC==null){
			// Radio is stopped, play!
			Log.d("RBT","Play");
			playRadio();
		}
		else{
			// Radio is playing, stop!
			Log.d("RBT","Stop");
			stopRadio();
		}

	}
	
	
	protected boolean playRadio(){
		// Shade button
		TextView tv = (TextView)PlayerActivity.current.findViewById(R.id.playbutton);
		tv.setEnabled(false);
		tv.setText("Cargando...");
		
		// Initialize the LibVLC multimedia framework.
        // This is required before doing anything with LibVLC.
        try {
        	PlayerActivity.mLibVLC = LibVLC.getInstance();
        	PlayerActivity.mLibVLC.init(PlayerActivity.current);
        } catch(LibVlcException e) {
            Toast.makeText(PlayerActivity.current,
                    "Error initializing the libVLC multimedia framework!",
                    Toast.LENGTH_LONG).show();
            PlayerActivity.current.finish();
        }
        
        // Set 1 second for network caching
        PlayerActivity.mLibVLC.setNetworkCaching(1000);
        
        // Load media list, clear it and add radio
        MediaList list = PlayerActivity.mLibVLC.getPrimaryMediaList();
        list.clear();
        list.add(LibVLC.PathToURI("http://live.radiobattletoads.com:443/saltxero.ogg"));
        
        // Finally, play!
        PlayerActivity.mLibVLC.playIndex(0);
        
        // Unshade button
        tv.setText("Vale, fue divertido. ¡Para!");
        tv.setEnabled(true);
        
        return true;
	}
	
	protected boolean stopRadio(){
		// Shade button
		TextView tv = (TextView)PlayerActivity.current.findViewById(R.id.playbutton);
		tv.setEnabled(false);
		tv.setText("Parando...");
		
		// Stop the multimedia framework
		PlayerActivity.mLibVLC.stop();
		PlayerActivity.mLibVLC.destroy();
		PlayerActivity.mLibVLC = null;
		
		// Unshade button
        tv.setText("¡Otra vez, otra vez!");
        tv.setEnabled(true);
		return true;
	}



}
