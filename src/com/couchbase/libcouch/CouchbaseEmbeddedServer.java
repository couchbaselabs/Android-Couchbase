package com.couchbase.libcouch;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Environment;
import android.os.IBinder;
import android.os.RemoteException;

/*
 * This is the minimal API for building against Android-Couchbase, its
 * main function is to allow developers to start Couchbase and contains
 * some utility functions
 */
public class CouchbaseEmbeddedServer {

	/* Tag used for log messages */
	public final static String TAG = "Couchbase";

	/* The application name (eg: com.dale.fubar) */
	public static String appNamespace;
	
	/* The name of the binary package of Couchbase stored in assets */
	private static String releaseName;

	/* 
	 * The default package name of couchdb binaries, applications are 
	 * recommended to use this default package name as it ensures this library
	 * was built to support these binaries
	 */
	private final static String defaultRelease = "couchbase-1.0-dp-be9fe2f";

	private static ICouchService couchService;
	private static ICouchClient couchClient;
	private static Context ctx;

	/* 
	 * A few of the utility functions require some of the same context
	 * that cannot be gotten automatically, so made this a class to 
	 * store some context for later functions
	 */
	public CouchbaseEmbeddedServer(Context appCtx, ICouchClient client) {
		couchClient = client;
		ctx = appCtx;
		appNamespace = ctx.getPackageName();		
	}	
	
	/* The path to this apps internal memory */
	public static String dataPath() {
		return "/data/data/" + CouchbaseEmbeddedServer.appNamespace;
	}

	/* The path to this apps external (sdcard) memory */
	public static String externalPath() {
		return Environment.getExternalStorageDirectory() + "/Android/data/" + CouchbaseEmbeddedServer.appNamespace;
	}	

	/* Start Couchbase with the default binaries */
	public ServiceConnection startCouchbase() {
		return startCouchbase(ctx, defaultRelease);
	}

	/* 
	 * Start Couchbase, this starts Couchbase as an android service, the ServiceConnection
	 * returned allowed for futher communication (such as install progress / started 
	 * callbacks), check the ICouchClient.aidl and ICouchServer.aidl for the definition
	 * of these callbacks
	 */
	public ServiceConnection startCouchbase(Context ctx, String release) {
		releaseName = release;		
		ctx.bindService(new Intent(ctx, CouchService.class), mConnection, Context.BIND_AUTO_CREATE);
		return mConnection;
	}

	/*
	 * This holds the connection to the CouchDB Service
	 */
	private final static ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, final IBinder service) {
			try {
				couchService = ICouchService.Stub.asInterface(service);
				couchService.initCouchDB(couchClient, releaseName);
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
}