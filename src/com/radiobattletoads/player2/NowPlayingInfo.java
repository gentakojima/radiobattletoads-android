package com.radiobattletoads.player2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class NowPlayingInfo {
	public String track_title;
	public String track_description;
	public String artwork_url;
	public Bitmap artwork_image;
	
	public String toString() {
		if (track_title != null && track_description != null) {
			return track_title + " - " + track_description;
		} else if (track_title != null) {
			return track_title;
		} else {
			return "";
		}
	}

	public Bitmap getCurrentArtwork(Context context) {
		if (artwork_image != null) {
			return artwork_image;
		} else {
			return BitmapFactory.decodeResource(
					context.getResources(),
					R.drawable.ic_launcher);
		}
	}
}
