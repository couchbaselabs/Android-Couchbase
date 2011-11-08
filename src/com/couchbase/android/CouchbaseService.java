package com.couchbase.android;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.Process;
import android.util.Log;

import com.couchbase.android.Intents.CouchbaseError;
import com.couchbase.android.Intents.CouchbaseStarted;

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
	 * Couchbase installation complete
	 */
	public static final int COMPLETE = 1;

	/**
	 * Couchbase startup complete
	 */
	public static final int COUCHBASE_STARTED = 2;

	/**
	 * System shell
	 */
	public static final String shell = "/system/bin/sh";

	/**
	 * The URL Couchbase is running on
	 */
	private static URL url;

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
				Exception e = (Exception) msg.obj;
				couchbaseError(e);
				break;
			case COMPLETE:
				couchbaseInstallThread = null;
				if(couchbaseRunThread == null)
					startCouchbaseService();
				else
					couchbaseStarted();
				break;
			case COUCHBASE_STARTED:
				url = (URL) msg.obj;
				couchbaseStarted();
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
		public void startCouchbase(ICouchbaseDelegate cb) {
			couchbaseDelegate = cb;

			if (couchbaseRunThread == null) {
				//if we're not running, trigger install
				//this will be no-op if already installed
				//and will trigger startup through handler
				installCouchbase();
			}
			else {
				couchbaseStarted();
			}
		}
	};

	/**
	 * Nofity the delegate that Couchbase has started
	 */
	void couchbaseStarted() {

		if(url != null) {
			//send broadcast intent
			Intent intent = new Intent(CouchbaseStarted.ACTION);
			intent.putExtra(CouchbaseStarted.HOST, url.getHost());
			intent.putExtra(CouchbaseStarted.PORT, url.getPort());
			intent.putExtra(CouchbaseStarted.PID, Process.myPid());
			getApplicationContext().sendBroadcast(intent);

			//notify delegate
			if (couchbaseDelegate != null) {
				couchbaseDelegate.couchbaseStarted(url.getHost(), url.getPort());
			}
		}
	}

	/**
	 * Notify the delegate that Couchbase has encountered an error
	 */
	void couchbaseError(Exception e) {
		StringWriter sw = new StringWriter();
		e.printStackTrace(new PrintWriter(sw));
		String stacktrace = sw.toString();

		//send broadcast intent
		Intent intent = new Intent(CouchbaseError.ACTION);
		intent.putExtra(CouchbaseError.MESSAGE, stacktrace);
		intent.putExtra(CouchbaseError.PID, Process.myPid());
		getApplicationContext().sendBroadcast(intent);

		//notify delegate
		if (couchbaseDelegate != null) {
			couchbaseDelegate.exit(stacktrace);
		}
	}

	/**
	 * Install Couchbase in a separate thread
	 */
	private void installCouchbase() {
		couchbaseInstallThread = new CouchbaseInstaller(getPackageCodePath(), mHandler);
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
		} catch (IOException e) {
			Log.v(CouchbaseMobile.TAG, "Error symlinking apk.ez", e);
			Message.obtain(mHandler, CouchbaseService.ERROR, e).sendToTarget();
			return;
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

}