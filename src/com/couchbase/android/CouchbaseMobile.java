package com.couchbase.android;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;

/*
 * This is the minimal API for building against Android-Couchbase, its
 * main function is to allow developers to start Couchbase and contains
 * some utility functions
 */
public class CouchbaseMobile {

	/* Tag used for log messages */
	public final static String TAG = "Couchbase";

	/* The application name (eg: com.dale.fubar) */
	public static String appNamespace;

	/* The name of the binary package of Couchbase stored in assets */
	private static String releaseName;

	/*
	 * The default package name of Couchbase binaries, applications are
	 * recommended to use this default package name as it ensures this library
	 * was built to support these binaries
	 */
	private final static String defaultRelease = "couchbase-1.0-dp-ebc0925";

	/*
	 * List of user defined ini files
	 */
	public static ArrayList<String> customIniFiles = new ArrayList<String>();

	private static ICouchbaseService couchbaseService;
	private static ICouchbaseDelegate couchbaseDelegate;
	private static Context ctx;

	/*
	 * A few of the utility functions require some of the same context
	 * that cannot be gotten automatically, so made this a class to
	 * store some context for later functions
	 */
	public CouchbaseMobile(Context appCtx, ICouchbaseDelegate delegate) {
		couchbaseDelegate = delegate;
		ctx = appCtx;
		appNamespace = ctx.getPackageName();
	}

	/* The path to this apps internal memory */
	public static String dataPath() {
		return "/data/data/" + CouchbaseMobile.appNamespace;
	}

	/* The path to this apps external (sdcard) memory */
	public static String externalPath() {
		return Environment.getExternalStorageDirectory() + "/Android/data/" + CouchbaseMobile.appNamespace;
	}

	/* Start Couchbase with the default binaries */
	public ServiceConnection startCouchbase() {
		return startCouchbase(ctx, defaultRelease);
	}

	/*
	 * Start Couchbase, this starts Couchbase as an android service, the ServiceConnection
	 * returned allowed for futher communication (such as install progress / started
	 * callbacks), check the ICouchbaseDelegate.aidl and ICouchbaseService.aidl for the definition
	 * of these callbacks
	 */
	public ServiceConnection startCouchbase(Context ctx, String release) {
		releaseName = release;
		ctx.bindService(new Intent(ctx, CouchbaseService.class), mConnection, Context.BIND_AUTO_CREATE);
		return mConnection;
	}

	/*
	 * This will copy a database from the assets folder into the
	 * couchbase database directory
	 * NOTE: Databases that use snappy encoding will not currently
	 * be able to be opened
	 */
	public void installDatabase(String fileName) throws IOException {
		File destination = new File(externalPath() + "/db/" + fileName);
		copyIffNotExists(fileName, destination);
	}

	/*
	 * Copy an .ini file from the assets folder into
	 * /data/data/com.your.app/user_data directory and
	 * add it to the list of ini files for couchbase to load
	 */
	public void copyIniFile(String fileName) throws IOException {
		File destination = new File(dataPath() + "/user_data/" + fileName);
		try {
			copyIffNotExists(fileName, destination);
			customIniFiles.add(destination.getAbsolutePath());
		} catch(FileNotFoundException e) {
			throw e;
		}
	}

	private void copyIffNotExists(String name, File destination) throws IOException  {
		if (!destination.exists()) {

			// Ensure directory exists
			(new File(destination.getParent())).mkdirs();

			AssetManager assetManager = ctx.getAssets();
			InputStream in = assetManager.open(name);
			OutputStream out = new FileOutputStream(destination);
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
	 * This holds the connection to the Couchbase Service
	 */
	private final static ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, final IBinder service) {
				couchbaseService = (ICouchbaseService)service;
				couchbaseService.startCouchbase(couchbaseDelegate, releaseName);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			couchbaseService = null;
		}
	};
}