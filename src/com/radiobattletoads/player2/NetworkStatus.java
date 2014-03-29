package com.radiobattletoads.player2;

import android.content.Context;
import android.net.ConnectivityManager;

public abstract class NetworkStatus {

	public static final int NETWORK_CONNECTED = 1;
	public static final int NETWORK_DISCONNECTED = 2;
	
	public static int getStatus(Context context){
		try{
			ConnectivityManager conMgr =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
			if(conMgr.getActiveNetworkInfo().isConnected() == false){
				return NETWORK_DISCONNECTED;
			}
			else{
				return NETWORK_CONNECTED;
			}
		}
		catch(NullPointerException e){
			// Some devices don't implement this, so just go on
			return NETWORK_CONNECTED;
		}
	}
	
	
}
