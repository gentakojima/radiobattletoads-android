package com.radiobattletoads.player2;

import java.io.IOException;
import java.io.InputStream;
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

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Message;
import android.util.Log;


public class DownloadCurrentinfo extends TimerTask {
	
	public final static int DOWNLOADCURRENTINFO_NEW = 1;
	public final static int DOWNLOADCURRENTINFO_DOWNLOADING = 2; 
	public final static int DOWNLOADCURRENTINFO_UPDATED = 3;
	public final static int DOWNLOADCURRENTINFO_IDLE = 4;
	public final static int DOWNLOADCURRENTINFO_FAILED = 5;
	
	public static String track_title;
	public static String track_description;
	public static String artwork_url;
	public static Bitmap artwork_image;
	
	public static int status;

	public DownloadCurrentinfo() {
		super();
		DownloadCurrentinfo.status = DOWNLOADCURRENTINFO_NEW;		
	}

	public void sendToActivity(){
		Log.d("RBT","Sending current info to activity");
		if(PlayerActivity.currentActivity==null) return;
		Message m = new Message();
		if(track_title!=null){
			m.what = PlayerActivity.MESSAGE_CURRENTPROGRAM;
			m.arg1 = DOWNLOADCURRENTINFO_UPDATED; // FIXME enviar siempre?
			Bundle data = new Bundle();
			data.putString("title", track_title);
			if(track_description!=null) data.putString("description", track_description);
			if(artwork_image!=null) data.putParcelable("artwork", artwork_image);
			m.setData(data);
			PlayerActivity.currentActivity.messageHandler.sendMessage(m);
		}
		else{
			// TODO maybe send failure downloading info?
		}
	}
	
	private boolean downloadInfo(){
		Log.d("RBT","Downloading info");
		HttpClient hc = new DefaultHttpClient();
    	HttpGet hg = new HttpGet("http://www.radiobattletoads.com/api/calendario.php?ahora=1&calendario=0");
    	
    	try {
    		
    		HttpResponse hr = hc.execute(hg);
			InputStream is = hr.getEntity().getContent();
			
			Document document;
			DocumentBuilderFactory builderFactory = DocumentBuilderFactory.newInstance();
			DocumentBuilder builder = null;
			
			try {
			    builder = builderFactory.newDocumentBuilder();
			} catch (ParserConfigurationException e) {
				e.printStackTrace();
				// TODO handle this exception
				Log.d("RBT","Exception downloading ParserConfigurationException :( " + e.getMessage());
				return false;
			}
			
			try {
			    document = builder.parse(is);
			} catch (SAXException e) {
			    e.printStackTrace();
			    // TODO handle this exception
			    Log.d("RBT","Exception downloading SAXException :( " + e.getMessage());
				return false;
			} catch (IOException e) {
			    e.printStackTrace();
			    // TODO handle this exception
			    Log.d("RBT","Exception downloading IOException :( " + e.getMessage());
				return false;
			}

			NodeList nodes = document.getElementsByTagName("programa");
			track_title = nodes.item(0).getFirstChild().getNodeValue();
			
			nodes = document.getElementsByTagName("episodio");
			track_description = nodes.item(0).getFirstChild().getNodeValue();
			
			nodes = document.getElementsByTagName("icono");
			String new_artwork_url = nodes.item(0).getFirstChild().getNodeValue();
			if(new_artwork_url != artwork_url){
				artwork_url = new_artwork_url;
				downloadArtwork();
			}
			return true;
			
    	} catch (Exception e) {
    		Log.d("RBT","Exception downloading :( " + e.getClass() + "---" + e.getMessage());
			// TODO handle this exception. DON'T RETUN TRUE!
			return true; 
		}

    }
	
	private boolean downloadArtwork(){
		Log.d("RBT","Downloading artwork");
		if(artwork_url==null){
			return false;
		}
	   	HttpClient hc = new DefaultHttpClient();
    	HttpGet hg = new HttpGet(artwork_url);
    	try {
			HttpResponse hr = hc.execute(hg);
			if(hr.getStatusLine().getStatusCode()==404){
				// TODO handle 404s (shouldn't exist, but...)
				Log.d("RBT","Exception downloading image - not found :( ");
				return false;
			}
			else{
				InputStream is = hr.getEntity().getContent();
				artwork_image = BitmapFactory.decodeStream(is);
				if(artwork_image == null){
					Log.d("RBT","Exception downloading image - trash :( ");
					// TODO handle downloading trash
					return false;
				}
			}
		} catch (IOException e) {
			Log.d("RBT","Exception downloading image :( " + e.getMessage());
			// TODO handle can't download
			return false;
		}
    	
    	return true;
		
    }
	
	@Override
	public void run() {
		if(downloadInfo()){
			Log.d("RBT","Downloadinfo returned true");
			sendToActivity();
		}
		else{
			Log.d("RBT","Downloadinfo returned FALSE");
		}
	}

}


