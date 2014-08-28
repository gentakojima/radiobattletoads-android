package com.radiobattletoads.player2;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;

public class NowPlayingInfo {
	public String track_title;
	public String track_chapter;
	public String artwork_url;
	public Bitmap artwork_image;
	public Bitmap background_image;
	public String track_desc;
	public String track_twitter;
	public String track_web;
	public Integer track_empezadohace;
	public Integer track_empiezaen;
	public String track_tipo;
	
	public String toString() {
		if (track_title != null && track_chapter != null) {
			return track_title + " - " + track_chapter;
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
	
	public Bitmap getCurrentBackground(Context context) {
		if (artwork_image != null) {
			return background_image;
		} else {
			return BitmapFactory.decodeResource(
					context.getResources(),
					R.drawable.ic_launcher);
		}
	}
	
	public String getCurrentTwitter(Context context){
		return track_twitter;
	}
	
	public String getCurrentDesc(Context context){
		return track_desc;
	}
	
	public String getCurrentWeb(Context context){
		return track_web;
	}

}
