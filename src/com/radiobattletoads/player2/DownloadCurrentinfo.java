package com.radiobattletoads.player2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.TimerTask;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;

import com.squareup.okhttp.apache.OkApacheClient;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.AsyncTask;
import android.util.Log;

public class DownloadCurrentinfo extends AsyncTask<String, Integer, Boolean> {

	static Boolean isRunning = Boolean.FALSE;
	private static List<DownloadCurrentInfoListener> listeners = new ArrayList<DownloadCurrentInfoListener>();
	public static void register(DownloadCurrentInfoListener listener) {
		if (!listeners.contains(listener)) {
			Log.d("RBT", "DownloadCurrentInfo: Registering listener");
			listeners.add(listener);
		}
	}
	
	public static void unRegister(DownloadCurrentInfoListener listener) {
		if (listeners.contains(listener)) {
			Log.d("RBT", "DownloadCurrentInfo: Unregistering listener");
			listeners.remove(listener);
		}
	}
	
	public final static int DOWNLOADCURRENTINFO_NEW = 1;
	public final static int DOWNLOADCURRENTINFO_DOWNLOADING = 2;
	public final static int DOWNLOADCURRENTINFO_UPDATED = 3;
	public final static int DOWNLOADCURRENTINFO_IDLE = 4;
	public final static int DOWNLOADCURRENTINFO_FAILED = 5;

	private static String track_title;
	private static String track_description;
	private static String artwork_url = "http://radiobattletoads.com/data/iconogrande-get.php";
	private static Bitmap artwork_image;
	
	protected static final HttpClient sHttpClient = new OkApacheClient();

	private Context context;

	public DownloadCurrentinfo(Context context) {
		this.context = context;
	}

	private boolean downloadInfo() {
		Log.d("RBT", "DownloadCurrentInfo: Downloading info");

		if (NetworkStatus.getStatus(this.context) == NetworkStatus.NETWORK_DISCONNECTED) {
			return false;
		}

		HttpGet hg = new HttpGet("http://radiobattletoads.com/data/emitiendo-get.php");

		try {
			HttpResponse hr = sHttpClient.execute(hg);
			InputStream is = hr.getEntity().getContent();
			InputStreamReader isr=new InputStreamReader(is);
			BufferedReader br=new BufferedReader(isr);
			String response = br.readLine();
			JSONObject jsonEmitiendo = new JSONObject(response);
			
			String track_title_new;
			try{
				track_title_new = jsonEmitiendo.getString("programa");
			}
			catch(Exception e){
				track_title_new = "Continuidad";
			}
			String track_description_new = jsonEmitiendo.getString("titulo");

			if (track_title == null || track_title_new.compareTo(track_title) != 0 || (track_description == null && track_description_new != null) || (track_description_new != null && track_description_new.compareTo(track_description) != 0)) {
				Log.d("RBT", "Different title and desc! " + track_title_new + "!=" + track_title + " OR " + track_description_new + "!=" + track_description);
				track_title = track_title_new;
				track_description = track_description_new;
				this.downloadArtwork();
				return true;
			} else {
				Log.d("RBT", "DownloadCurrentInfo: Same title and desc!");
				return false;
			}

		} catch (Exception e) {
			Log.d("RBT", "DownloadCurrentInfo: Exception downloading :( " + e.getClass() + "---" + e.getMessage());
			// TODO handle this exception. DON'T RETUN TRUE!
			return true;
		}

	}

	private boolean downloadArtwork() {
		Log.d("RBT", "DownloadCurrentInfo: Downloading artwork");
		if (artwork_url == null) {
			return false;
		}
		HttpClient hc = new DefaultHttpClient();
		String ALLOWED_URI_CHARS = "@#&=*+-_.,:!?()/~'%";
		artwork_url = Uri.encode(artwork_url, ALLOWED_URI_CHARS);
		HttpGet hg = new HttpGet(artwork_url);
		try {
			HttpResponse hr = hc.execute(hg);
			if (hr.getStatusLine().getStatusCode() == 404) {
				// TODO handle 404s (shouldn't exist, but...)
				Log.d("RBT", "Exception downloading image - not found :( ");
				return false;
			} else {
				InputStream is = hr.getEntity().getContent();
				artwork_image = BitmapFactory.decodeStream(is);
				if (artwork_image == null) {
					Log.d("RBT", "Exception downloading image - trash :( ");
					// TODO handle downloading trash
					return false;
				}
			}
		} catch (IOException e) {
			Log.d("RBT", "Exception downloading image :( " + e.getMessage());
			// TODO handle can't download
			return false;
		}

		return true;

	}

	@Override
	protected Boolean doInBackground(String... params) {
		// Non UI thread
		DownloadCurrentinfo.isRunning = Boolean.TRUE;
		return downloadInfo();
	}

	protected void onPostExecute(Boolean result) {
		// UI thread (no need for a handler)
		DownloadCurrentinfo.isRunning = Boolean.FALSE;
		if (DownloadCurrentinfo.listeners.size() > 0) {
			if (result) {
				NowPlayingInfo info = new NowPlayingInfo();
				info.track_title = track_title;
				info.track_description = track_description;
				info.artwork_url = artwork_url;
				info.artwork_image = artwork_image;
				RBTPlayerApplication.getFromContext(context).setCachedNowPlayingInfo(info);
				RBTPlayerApplication.getFromContext(context).getNotifications().updateNotification();
				for (int i=0;i<DownloadCurrentinfo.listeners.size();i++) {
					DownloadCurrentinfo.listeners.get(i).onPlayingInformationChange(info);
				}
			} else {
				if(track_title==null){
					for (int i=0;i<DownloadCurrentinfo.listeners.size();i++) {
						DownloadCurrentinfo.listeners.get(i).onPlayingInformationDownloadError();
					}
				}
			}
		}
	}

	public static interface DownloadCurrentInfoListener {
		// List of methods reflecting task results
		void onPlayingInformationChange(NowPlayingInfo newInfo);

		void onPlayingInformationDownloadError();
	}

	public static TimerTask getTimerTask(final Context context) {
		return new TimerTask() {

			@Override
			public void run() {
				// If already running, skip launching a new task
				if (DownloadCurrentinfo.isRunning) {
					return;
				}
				Log.d("RBT","Downloadcurrentinfo gettimertask");
				DownloadCurrentinfo task = new DownloadCurrentinfo(context);
				task.execute();
			}
		};
	}

}
