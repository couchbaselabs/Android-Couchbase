package com.couchbase.android;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.util.ArrayList;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.res.AssetManager;
import android.os.Environment;
import android.os.IBinder;
import android.util.Log;

/**
 * This is the minimal API for building against Android-Couchbase, its
 * main function is to allow developers to start Couchbase and contains
 * some utility functions
 *
 */
public class CouchbaseMobile {

	/**
	 * Tag used for log messages
	 */
	public final static String TAG = "Couchbase";

	/**
	 * The application name (eg: com.dale.fubar)
	 */
	private static String appNamespace;

	/**
	 * List of user defined ini files
	 */
	private static ArrayList<String> customIniFiles = new ArrayList<String>();

	/**
	 * Reference to the couchbaseService
	 */
	private static ICouchbaseService couchbaseService;

	/**
	 * Reference to the delegate
	 */
	private static ICouchbaseDelegate couchbaseDelegate;

	/**
	 * Reference to the Android context
	 */
	private static Context ctx;

	/**
	 * Couchbase Mobile Version Number
	 */
	private static String version;

	/**
	 *
	 */
	public CouchbaseMobile(Context appCtx) {
		ctx = appCtx;
		appNamespace = ctx.getPackageName();
	}

	/**
	 * A few of the utility functions require some of the same context
	 * that cannot be gotten automatically, so made this a class to
	 * store some context for later functions
	 *
	 * @param appCtx the Android context
	 * @param delegate the Couchbase delegate
	 */
	public CouchbaseMobile(Context appCtx, ICouchbaseDelegate delegate) {
		couchbaseDelegate = delegate;
		ctx = appCtx;
		appNamespace = ctx.getPackageName();
	}

	/**
	 * The path to this apps internal memory
	 *
	 * @return path to apps internal directory
	 */
	public static String dataPath() {
		return "/data/data/" + CouchbaseMobile.appNamespace;
	}

	/**
	 * The path to this apps external (sdcard) memory
	 *
	 * @return path to the apps external directory
	 */
	public static String externalPath() {
		return Environment.getExternalStorageDirectory() + "/Android/data/" + CouchbaseMobile.appNamespace;
	}

	/**
	 * Start Couchbase with the default binaries
	 *
	 * @return the Couchbase service connection
	 */
	public ServiceConnection startCouchbase() {
		return startCouchbase(ctx);
	}

	/**
	 * Start Couchbase, this starts Couchbase as an android service, the ServiceConnection
	 * returned allowed for futher communication (such as install progress / started
	 * callbacks)
	 *
	 * @see ICouchbaseDelegate
	 * @see ICouchbaseService
	 *
	 * @param ctx the Android context
	 * @return Couchbase service connection
	 */
	public ServiceConnection startCouchbase(Context ctx) {
		ctx.bindService(new Intent(ctx, CouchbaseService.class), mConnection, Context.BIND_AUTO_CREATE);
		return mConnection;
	}

	/**
	 * This will copy a database from the assets folder into the
	 * couchbase database directory
	 * NOTE: Databases that use snappy encoding will not currently
	 * be able to be opened
	 *
	 * @param fileName the name of the data file
	 * @throws IOException
	 */
	public void installDatabase(String fileName) throws IOException {
		File destination = new File(externalPath() + "/db/" + fileName);
		copyIffNotExists(fileName, destination);
	}

	/**
	 * Copy an .ini file from the assets folder into
	 * /data/data/com.your.app/user_data directory and
	 * add it to the list of ini files for couchbase to load
	 *
	 * @param fileName the name of the ini file
	 * @throws IOException
	 */
	public void copyIniFile(String fileName) throws IOException {
		File destination = new File(dataPath() + "/user_data/" + fileName);
		copyIffNotExists(fileName, destination);
		customIniFiles.add(destination.getAbsolutePath());
	}

	/**
	 * Copy a file from the assets folder to a destination, if it does not already exist
	 *
	 * @param name the name of the file in the assets folder
	 * @param destination destination file
	 * @throws IOException
	 */
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

	/**
	 * Get the custom INI files to be used when starting Couchbase
	 *
	 * @return array of string paths to ini files
	 */
	public static ArrayList<String> getCustomIniFiles() {
		return customIniFiles;
	}

	/**
	 * Get the application namespace
	 *
	 * @return string containing the application namespace
	 */
	public static String getAppNamespace() {
		return appNamespace;
	}

	/**
	 * This holds the connection to the Couchbase Service
	 */
	private final static ServiceConnection mConnection = new ServiceConnection() {
		@Override
		public void onServiceConnected(ComponentName className, final IBinder service) {
				couchbaseService = (ICouchbaseService)service;
				couchbaseService.startCouchbase(couchbaseDelegate);
		}

		@Override
		public void onServiceDisconnected(ComponentName className) {
			couchbaseService = null;
		}
	};

	public String getVersion() {
	    String result = "unknown";

	    if(ctx != null  && version == null) {
	        try {
	            InputStream is = ctx.getAssets().open("CouchbaseVersion.txt");
	            BufferedReader br = new BufferedReader(new InputStreamReader(is));
	            version = br.readLine();
	            result = version;
	            br.close();
	            is.close();
	        }
	        catch(IOException e) {
	            Log.e(TAG, "Unabled to read CouchbaseVersion.txt", e);
	        }
	    }
	    else {
	        result = version;
	    }

	    return result;
	}
}
