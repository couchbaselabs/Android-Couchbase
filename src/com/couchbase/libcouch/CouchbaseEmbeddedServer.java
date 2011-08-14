package com.couchbase.libcouch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
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
	private final static String defaultRelease = "couchbase-test-60d0c5d";

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
	 * This will copy a database from the assets folder into the
	 * couch database directory
	 * NOTE: Databases that use snappy encoding will not currently
	 * be able to be opened
	 */
	public void installDatabase(String fileName) throws IOException {
		File db = new File(externalPath() + "/db/" + fileName);
		if (!db.exists()) {
			
			// Ensure db directory exists
			(new File(externalPath() + "/db/")).mkdirs();
			
			AssetManager assetManager = ctx.getAssets();
			InputStream in = assetManager.open(fileName);
			OutputStream out = new FileOutputStream(db);
			byte[] buffer = new byte[1024];
			int read;
			while((read = in.read(buffer)) != -1){
				out.write(buffer, 0, read);
			}
			in.close();
			out.close();
		}
	}

	/*
	 * This holds the connection to the CouchDB Service
	 */
	private final static ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, final IBinder service) {
			try {
				couchService = ICouchService.Stub.asInterface(service);
				couchService.startCouchbase(couchClient, releaseName);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			couchService = null;
		}
	};
}