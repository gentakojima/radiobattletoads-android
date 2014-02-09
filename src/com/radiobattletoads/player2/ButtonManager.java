package com.radiobattletoads.player2;

import android.content.Intent;
import android.util.Log;
import android.view.View;
import android.view.View.OnClickListener;
public class ButtonManager implements OnClickListener {

	public void onClick(View v) {
		
		Log.d("RBT","Click event!");
		
		PlayerActivity w = PlayerActivity.currentActivity;
		
		if(PlayerService.status==PlayerService.PLAYER_UNINITIALIZED){
			// Initialize and play!
			Log.d("RBT","Play");
			PlayerActivity.currentActivity.startService(new Intent(w, PlayerService.class));
		}
		else{
			// Radio is playing, stop!
			Log.d("RBT","Stop");
			PlayerActivity.currentActivity.stopService(new Intent(w, PlayerService.class));
		}

	}
	
	
	



}
