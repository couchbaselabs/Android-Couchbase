package com.couchbase.libcouch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.IBinder;
import android.os.RemoteException;

public class CouchDB {

	private static String releaseUrl;
	private static String releaseName;

	private static ICouchService couchService;
	private static ICouchClient couchClient;

	/*
	 * This holds the connection to the CouchDB Service
	 */
	private final static ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, final IBinder service) {
			try {
				couchService = ICouchService.Stub.asInterface(service);
				couchService.initCouchDB(couchClient, releaseUrl, releaseName);
			} catch (RemoteException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			couchService = null;
		}
	};

	public static ServiceConnection getService(Context ctx, String url, String release, ICouchClient client) {
		releaseUrl = url;
		releaseName = release;
		couchClient = client;
		ctx.bindService(new Intent(ctx, CouchService.class), mConnection, Context.BIND_AUTO_CREATE);
		return mConnection;
	}

}