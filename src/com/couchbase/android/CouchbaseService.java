package com.couchbase.android;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

/**
 * Implementation of the Couchbase service
 *
 */

public class CouchbaseService extends Service {

	/**
	 * Couchbase encountered an error
	 */
	public static final int ERROR = 0;

	/**
	 * Couchbase installer has made progress
	 */
	public static final int PROGRESS = 1;

	/**
	 * Couchbase installation complete
	 */
	public static final int COMPLETE = 2;

	/**
	 * Couchbase startup complete
	 */
	public static final int COUCHBASE_STARTED = 3;

	/**
	 * System shell
	 */
	public static final String shell = "/system/bin/sh";

	/**
	 * The URL Couchbase is running on
	 */
	private static URL url;

	/**
	 * Has Couchbase started
	 */
	private boolean couchbaseStarted = false;

	/**
	 * Is Couchbase Stopping
	 */
	private boolean couchbaseStopping = false;

	/**
	 * Delegate used to notify the client of events in the service
	 */
	private ICouchbaseDelegate couchbaseDelegate;

	/**
	 * Thread responsible for installing Couchbase
	 */
	private CouchbaseInstaller couchbaseInstallThread;

	/**
	 * Thread responsible for communicating with Couchbase process
	 */
	private static Thread couchbaseRunThread;

	/**
	 *	A handler to pass messages between Couchbase main thread, Couchbase installer thread, and application main thread
	 *
	 *  Couchbase runs in a seperate thread, have the communication run
	 *  through our own handler so the app doesnt need to create a new
	 *  handler to deal with touching the UI from a different thread
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case ERROR:
				if(msg.obj instanceof Exception) {
					Exception e = (Exception) msg.obj;
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String stacktrace = sw.toString();
					if (couchbaseDelegate != null) {
						couchbaseDelegate.exit(stacktrace);
					}
				}
				else if(msg.obj instanceof String){
					if (couchbaseDelegate != null) {
						couchbaseDelegate.exit((String)msg.obj);
					}
				}
				break;
			case PROGRESS:
				if (couchbaseDelegate != null) {
					couchbaseDelegate.installing(msg.arg1, msg.arg2);
				}
				break;
			case COMPLETE:
				couchbaseInstallThread = null;
				if(couchbaseRunThread == null)
					startCouchbaseService();
				else
					if (couchbaseDelegate != null) {
						couchbaseDelegate.couchbaseStarted(url.getHost(),
								url.getPort());
					}
				break;
			case COUCHBASE_STARTED:
				url = (URL) msg.obj;
				
				if (couchbaseDelegate != null) {
					couchbaseDelegate.couchbaseStarted(url.getHost(),
							url.getPort());
				}
				break;
			}
		}
	};

	/**
	 * This is called when the service is destroyed
	 */
	@Override
	public void onDestroy() {
		couchbaseDelegate = null;
	}

	/**
	 * This is called called on the initial service binding
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return new CouchbaseServiceImpl();
	}

	/**
	 * An implementation of the Couchbase service exposed to clients wrapping around this implementation
	 *
	 */
	public class CouchbaseServiceImpl extends Binder implements ICouchbaseService {

		@Override
		public void startCouchbase(ICouchbaseDelegate cb, final String pkg) {
			couchbaseDelegate = cb;
			if (!CouchbaseInstaller.checkInstalled(pkg)) {
				installCouchbase(pkg);
			} else {
				if (couchbaseStarted == true) {
					couchbaseStarted();
				} else {
					if(couchbaseRunThread == null)
						startCouchbaseService();
					else
						if (couchbaseDelegate != null) {
							couchbaseDelegate.couchbaseStarted(url.getHost(),
									url.getPort());
						}
				}
			}
		}

		/*
		 */
		@Override
		public void stopCouchbase() {

		}
	};

	/**
	 * Nofity the delegate that Couchbase has started
	 */
	void couchbaseStarted() {
		if (couchbaseDelegate != null) {
			couchbaseDelegate.couchbaseStarted(url.getHost(), url.getPort());
		}
	}

	/**
	 * Install Couchbase in a separate thrad
	 *
	 * @param pkg the package identifier to verify and install if necessary
	 */
	private void installCouchbase(final String pkg) {
		couchbaseInstallThread = new CouchbaseInstaller(pkg, mHandler, this);
		couchbaseInstallThread.start();
	}

	/**
	 * Start the Couchbase service in a separate thread
	 */
	void startCouchbaseService() {
		String path = CouchbaseMobile.dataPath();
		String apkPath = getPackageCodePath();
		
		try {
			new File(path + "/apk.ez").delete();
			Runtime.getRuntime().exec(new String[] { "ln", "-s", apkPath, path + "/apk.ez" });
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		String[] plainArgs = {
				"beam", "-K", "true", "--", 
				"-noinput",
				"-boot_var", "APK", path + "/apk.ez",
				"-kernel", "inetrc", "\""+ path + "/erlang/bin/erl_inetrc\"",
				"-native_lib_path", path + "/lib",
				"-sasl", "errlog_type", "all",
				"-boot", path + "/erlang/bin/start",
				"-root", path + "/apk.ez/assets",
				"-eval", "code:load_file(jninif), R = application:start(couch), io:format(\"~w~n\",[R]).",
				"-couch_ini",
				path + "/couchdb/etc/couchdb/default.ini",
				path + "/couchdb/etc/couchdb/local.ini",
				path + "/couchdb/etc/couchdb/android.default.ini"};

		ArrayList<String> args = new ArrayList<String>(Arrays.asList(plainArgs));
		for (String iniPath : CouchbaseMobile.getCustomIniFiles()) {
			args.add(iniPath);
		}

		args.add(path + "/couchdb/etc/couchdb/overrides.ini");
		final String[] argv = args.toArray(new String[args.size()]);
		final String sopath = path + "/lib/libbeam.so";
		final String bindir = path + "/erlang/bin";
		couchbaseRunThread = new Thread() {
			public void run() {
				ErlangThread.setHandler(mHandler);
				ErlangThread.start_erlang(bindir, sopath, argv);
				Log.i(CouchbaseMobile.TAG, "Erlang thread ended.");
			}
		};
		couchbaseRunThread.start();
	}


	/**
	 * Utility function to join a collection of strings with a delimiter
	 *
	 * @param a collection of strings to join
	 * @param delimiter string to insert between joined strings
	 * @return the joined string
	 */
	public static String join(Collection<String> s, String delimiter) {
		StringBuffer buffer = new StringBuffer();
		Iterator<String> iter = s.iterator();
		while (iter.hasNext()) {
			buffer.append(iter.next());
			if (iter.hasNext()) {
				buffer.append(delimiter);
			}
		}
		return buffer.toString();
	}

}