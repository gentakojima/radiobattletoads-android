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
import android.graphics.Canvas;
import android.graphics.ColorMatrix;
import android.graphics.ColorMatrixColorFilter;
import android.graphics.Paint;
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
	private static String track_chapter;
	private static String artwork_url = "http://radiobattletoads.com/data/iconogrande-get.php";
	private static Bitmap artwork_image;
	private static Bitmap background_image;
	private static String track_twitter;
	private static String track_web;
	private static String track_desc;
	private static Integer track_empezadohace;
	private static Integer track_empiezaen;
	private static String track_tipo;
	
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

			if (track_title == null || track_title_new.compareTo(track_title) != 0 || (track_chapter == null && track_description_new != null) || (track_description_new != null && track_description_new.compareTo(track_chapter) != 0)) {
				Log.d("RBT", "Different title and desc! " + track_title_new + "!=" + track_title + " OR " + track_description_new + "!=" + track_chapter);
				track_title = track_title_new;
				track_chapter = track_description_new;
				try{
					track_desc = jsonEmitiendo.getString("descripcion");
				}
				catch(Exception e){
					track_desc = null;
				}
				try{
					track_twitter = jsonEmitiendo.getString("twitter");
				}catch(Exception e){
					track_twitter = null;
				}
				try{
					track_web = jsonEmitiendo.getString("web");
				}
				catch(Exception e){
					track_web = null;
				}
				track_tipo = jsonEmitiendo.getString("tipo");
				try{
					track_empezadohace = jsonEmitiendo.getInt("empezadohace");
				}
				catch(Exception e){
					track_empezadohace = null;
				}
				try{
					track_empiezaen = jsonEmitiendo.getInt("empiezaen");
				}
				catch(Exception e){
					track_empiezaen = null;
				}
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
				else{
					background_image = this.fastblur(Bitmap.createScaledBitmap(artwork_image, 600, 600, true),35);
					background_image = this.changeBitmapContrastBrightness(background_image, 0.6f,-40f);
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
				info.track_chapter = track_chapter;
				info.artwork_url = artwork_url;
				info.artwork_image = artwork_image;
				info.background_image = background_image;
				info.track_twitter = track_twitter;
				info.track_desc = track_desc;
				info.track_web = track_web;
				info.track_empiezaen = track_empiezaen;
				info.track_empezadohace = track_empezadohace;
				info.track_tipo = track_tipo;
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
	
	private Bitmap fastblur(Bitmap sentBitmap, int radius) {

        // Stack Blur v1.0 from
        // http://www.quasimondo.com/StackBlurForCanvas/StackBlurDemo.html
        //
        // Java Author: Mario Klingemann <mario at quasimondo.com>
        // http://incubator.quasimondo.com
        // created Feburary 29, 2004
        // Android port : Yahel Bouaziz <yahel at kayenko.com>
        // http://www.kayenko.com
        // ported april 5th, 2012

        // This is a compromise between Gaussian Blur and Box blur
        // It creates much better looking blurs than Box Blur, but is
        // 7x faster than my Gaussian Blur implementation.
        //
        // I called it Stack Blur because this describes best how this
        // filter works internally: it creates a kind of moving stack
        // of colors whilst scanning through the image. Thereby it
        // just has to add one new block of color to the right side
        // of the stack and remove the leftmost color. The remaining
        // colors on the topmost layer of the stack are either added on
        // or reduced by one, depending on if they are on the right or
        // on the left side of the stack.
        //
        // If you are using this algorithm in your code please add
        // the following line:
        //
        // Stack Blur Algorithm by Mario Klingemann <mario@quasimondo.com>

        Bitmap bitmap = sentBitmap.copy(sentBitmap.getConfig(), true);

        if (radius < 1) {
            return (null);
        }

        int w = bitmap.getWidth();
        int h = bitmap.getHeight();

        int[] pix = new int[w * h];
        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.getPixels(pix, 0, w, 0, 0, w, h);

        int wm = w - 1;
        int hm = h - 1;
        int wh = w * h;
        int div = radius + radius + 1;

        int r[] = new int[wh];
        int g[] = new int[wh];
        int b[] = new int[wh];
        int rsum, gsum, bsum, x, y, i, p, yp, yi, yw;
        int vmin[] = new int[Math.max(w, h)];

        int divsum = (div + 1) >> 1;
        divsum *= divsum;
        int dv[] = new int[256 * divsum];
        for (i = 0; i < 256 * divsum; i++) {
            dv[i] = (i / divsum);
        }

        yw = yi = 0;

        int[][] stack = new int[div][3];
        int stackpointer;
        int stackstart;
        int[] sir;
        int rbs;
        int r1 = radius + 1;
        int routsum, goutsum, boutsum;
        int rinsum, ginsum, binsum;

        for (y = 0; y < h; y++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            for (i = -radius; i <= radius; i++) {
                p = pix[yi + Math.min(wm, Math.max(i, 0))];
                sir = stack[i + radius];
                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);
                rbs = r1 - Math.abs(i);
                rsum += sir[0] * rbs;
                gsum += sir[1] * rbs;
                bsum += sir[2] * rbs;
                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }
            }
            stackpointer = radius;

            for (x = 0; x < w; x++) {

                r[yi] = dv[rsum];
                g[yi] = dv[gsum];
                b[yi] = dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (y == 0) {
                    vmin[x] = Math.min(x + radius + 1, wm);
                }
                p = pix[yw + vmin[x]];

                sir[0] = (p & 0xff0000) >> 16;
                sir[1] = (p & 0x00ff00) >> 8;
                sir[2] = (p & 0x0000ff);

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[(stackpointer) % div];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi++;
            }
            yw += w;
        }
        for (x = 0; x < w; x++) {
            rinsum = ginsum = binsum = routsum = goutsum = boutsum = rsum = gsum = bsum = 0;
            yp = -radius * w;
            for (i = -radius; i <= radius; i++) {
                yi = Math.max(0, yp) + x;

                sir = stack[i + radius];

                sir[0] = r[yi];
                sir[1] = g[yi];
                sir[2] = b[yi];

                rbs = r1 - Math.abs(i);

                rsum += r[yi] * rbs;
                gsum += g[yi] * rbs;
                bsum += b[yi] * rbs;

                if (i > 0) {
                    rinsum += sir[0];
                    ginsum += sir[1];
                    binsum += sir[2];
                } else {
                    routsum += sir[0];
                    goutsum += sir[1];
                    boutsum += sir[2];
                }

                if (i < hm) {
                    yp += w;
                }
            }
            yi = x;
            stackpointer = radius;
            for (y = 0; y < h; y++) {
                // Preserve alpha channel: ( 0xff000000 & pix[yi] )
                pix[yi] = ( 0xff000000 & pix[yi] ) | ( dv[rsum] << 16 ) | ( dv[gsum] << 8 ) | dv[bsum];

                rsum -= routsum;
                gsum -= goutsum;
                bsum -= boutsum;

                stackstart = stackpointer - radius + div;
                sir = stack[stackstart % div];

                routsum -= sir[0];
                goutsum -= sir[1];
                boutsum -= sir[2];

                if (x == 0) {
                    vmin[y] = Math.min(y + r1, hm) * w;
                }
                p = x + vmin[y];

                sir[0] = r[p];
                sir[1] = g[p];
                sir[2] = b[p];

                rinsum += sir[0];
                ginsum += sir[1];
                binsum += sir[2];

                rsum += rinsum;
                gsum += ginsum;
                bsum += binsum;

                stackpointer = (stackpointer + 1) % div;
                sir = stack[stackpointer];

                routsum += sir[0];
                goutsum += sir[1];
                boutsum += sir[2];

                rinsum -= sir[0];
                ginsum -= sir[1];
                binsum -= sir[2];

                yi += w;
            }
        }

        Log.e("pix", w + " " + h + " " + pix.length);
        bitmap.setPixels(pix, 0, w, 0, 0, w, h);

        return (bitmap);
    }
	
	/**
	 * 
	 * @param bmp input bitmap
	 * @param contrast 0..10 1 is default
	 * @param brightness -255..255 0 is default
	 * @return new bitmap
	 */
	private Bitmap changeBitmapContrastBrightness(Bitmap bmp, float contrast, float brightness)
	{
	    ColorMatrix cm = new ColorMatrix(new float[]
	            {
	                contrast, 0, 0, 0, brightness,
	                0, contrast, 0, 0, brightness,
	                0, 0, contrast, 0, brightness,
	                0, 0, 0, 1, 0
	            });

	    Bitmap ret = Bitmap.createBitmap(bmp.getWidth(), bmp.getHeight(), bmp.getConfig());

	    Canvas canvas = new Canvas(ret);

	    Paint paint = new Paint();
	    paint.setColorFilter(new ColorMatrixColorFilter(cm));
	    canvas.drawBitmap(bmp, 0, 0, paint);

	    return ret;
	}
	

}
