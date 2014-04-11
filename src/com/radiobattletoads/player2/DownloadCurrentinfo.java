package com.radiobattletoads.player2;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TimerTask;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;

import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import com.radiobattletoads.player2.PlayerService.PlayerStatusChangeListener;
import com.squareup.okhttp.OkHttpClient;
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
			listeners.add(listener);
		}
	}
	
	public static void unRegister(DownloadCurrentInfoListener listener) {
		if (listeners.contains(listener)) {
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
	private static String artwork_url;
	private static Bitmap artwork_image;
	
	protected static final HttpClient sHttpClient = new OkApacheClient();

	private Context context;

	public DownloadCurrentinfo(Context context) {
		this.context = context;
	}

	private boolean downloadInfo() {
		Log.d("RBT", "Downloading info");

		if (NetworkStatus.getStatus(this.context) == NetworkStatus.NETWORK_DISCONNECTED) {
			return false;
		}

		HttpGet hg = new HttpGet("http://www.radiobattletoads.com/api/calendario.php?ahora=1&calendario=0");

		try {

			HttpResponse hr = sHttpClient.execute(hg);
			InputStream is = hr.getEntity().getContent();

			Document document;
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = null;

			try {
				builder = builderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				// TODO handle this exception
				Log.d("RBT", "Exception downloading ParserConfigurationException :( " + e.getMessage());
				return false;
			}

			try {
				document = builder.parse(is);
			} catch (SAXException e) {
				e.printStackTrace();
				// TODO handle this exception
				Log.d("RBT", "Exception downloading SAXException :( " + e.getMessage());
				return false;
			} catch (IOException e) {
				e.printStackTrace();
				// TODO handle this exception
				Log.d("RBT", "Exception downloading IOException :( " + e.getMessage());
				return false;
			}

			NodeList nodes = document.getElementsByTagName("programa");
			String track_title_new = nodes.item(0).getFirstChild().getNodeValue();

			nodes = document.getElementsByTagName("episodio");
			String track_description_new = null;
			try {
				track_description_new = nodes.item(0).getFirstChild().getNodeValue();
			} catch (NullPointerException e) {
				// TODO no episode name, do something?
			}

			nodes = document.getElementsByTagName("icono");
			String new_artwork_url = nodes.item(0).getFirstChild().getNodeValue();

			if (track_title == null || track_title_new.compareTo(track_title) != 0 || (track_description == null && track_description_new != null) || (track_description_new != null && track_description_new.compareTo(track_description) != 0)) {
				Log.d("RBT", "Different title and desc! " + track_title_new + "!=" + track_title + " OR " + track_description_new + "!=" + track_description);
				track_title = track_title_new;
				track_description = track_description_new;
				if (artwork_url == null || new_artwork_url.compareTo(artwork_url) != 0) {
					artwork_url = new_artwork_url;
					downloadArtwork();
				}
				return true;
			} else {
				Log.d("RBT", "Same title and desc!");
				return false;
			}

		} catch (Exception e) {
			Log.d("RBT", "Exception downloading :( " + e.getClass() + "---" + e.getMessage());
			// TODO handle this exception. DON'T RETUN TRUE!
			return true;
		}

	}

	private boolean downloadArtwork() {
		Log.d("RBT", "Downloading artwork");
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
				DownloadCurrentinfo task = new DownloadCurrentinfo(context);
				task.execute();
			}
		};
	}

}
