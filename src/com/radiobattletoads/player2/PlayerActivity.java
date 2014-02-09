package com.radiobattletoads.player2;

import org.videolan.libvlc.LibVLC;
import android.os.Bundle;
import android.app.Activity;

public class PlayerActivity extends Activity{
	
	static LibVLC mLibVLC = null;
	static Activity current = null;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.player_layout);
		
		current = this;
		
		findViewById(R.id.playbutton).setOnClickListener(new ButtonManager());

	}



}
