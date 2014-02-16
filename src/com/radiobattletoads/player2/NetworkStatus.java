package com.radiobattletoads.player2;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

public abstract class NetworkStatus {

	public static final int NETWORK_CONNECTED = 1;
	public static final int NETWORK_DISCONNECTED = 2;
	
	public static int getStatus(Context context){
		ConnectivityManager conMgr =  (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
		if(conMgr.getNetworkInfo(0).getState() != NetworkInfo.State.CONNECTED &&
				conMgr.getNetworkInfo(1).getState() != NetworkInfo.State.CONNECTED ){
			return NETWORK_DISCONNECTED;
		}
		else{
			return NETWORK_CONNECTED;
		}
	}
	
	
}
