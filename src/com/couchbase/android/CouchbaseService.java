package com.couchbase.android;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.app.Service;
import android.content.Intent;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.util.Log;

import com.google.ase.Exec;

public class CouchbaseService extends Service {

	/*
	 * Contants used to communnicate between the Couchbase Service
	 * and this thread
	 */
	public final static int ERROR = 0;
	public final static int PROGRESS = 1;
	public final static int COMPLETE = 2;
	public final static int COUCHBASE_STARTED = 3;
	public final static int INSTALLING = 4;

	/* The url Couchbase has started on */
	public URL url;

	/* Has Couchbase started */
	public boolean couchbaseStarted = false;

	/* This is how we talk to the app that started Couchbase */
	private ICouchbaseDelegate couchbaseDelegate;

	private Integer pid;
	private PrintStream out;
	private BufferedReader in;

	/*
	 * Couchbase runs in a seperate thread, have the communication run
	 * through our own handler so the app doesnt need to create a new
	 * handler to deal with touching the UI from a different thread
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {

			switch (msg.what) {
			case ERROR:
				Exception e = (Exception) msg.obj;
				StringWriter sw = new StringWriter();
				e.printStackTrace(new PrintWriter(sw));
				String stacktrace = sw.toString();
				if (couchbaseDelegate != null) {
					couchbaseDelegate.exit(stacktrace);
				}
				break;
			case PROGRESS:
				couchbaseDelegate.installing(msg.arg1, msg.arg2);
				break;
			case COMPLETE:
				startCouchbaseService();
				break;
			case COUCHBASE_STARTED:
				URL url = (URL) msg.obj;
				couchbaseDelegate.couchbaseStarted(url.getHost(), url.getPort());
				break;
			}
		}
	};

	/*
	 * This is called when the service is destroyed
	 */
	@Override
	public void onDestroy() {
		if (couchbaseStarted) {
			stop();
		}
		couchbaseDelegate = null;
	}

	/*
	 * This is called called on the initial service binding
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return new CouchbaseServiceImpl();
	}

	/*
	 * implements the callbacks that clients can call into the couchbase service
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
					startCouchbaseService();
				}
			}
		}

		/*
		 */
		@Override
		public void stopCouchbase() {
			stop();
		}
	};

	/* once couchbase has started we need to notify the waiting client */
	void couchbaseStarted() {
		couchbaseDelegate.couchbaseStarted(url.getHost(), url.getPort());
	}

	/* Install Couchbase in a seperate thread */
	private void installCouchbase(final String pkg) {
		final CouchbaseService service = this;
		new Thread() {
			@Override
			public void run() {
				try {
					CouchbaseInstaller.doInstall(pkg, mHandler, service);
				} catch (FileNotFoundException e) {
					Message.obtain(mHandler, CouchbaseService.ERROR, e).sendToTarget();
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/* Start the couchbase process, run in a seperate thread */
	void startCouchbaseService() {

		String path = CouchbaseMobile.dataPath();

		String cmd = path + "/couchdb/bin/couchdb_wrapper";

		String[] plainArgs = {
				"+Bd", "-noinput", "sasl", "errlog_type", "all", "+K", "true",
				"-env", "ERL_LIBS", path + "/couchdb/lib/couchdb/erlang/lib", "-couch_ini",
				path + "/couchdb/etc/couchdb/default.ini",
				path + "/couchdb/etc/couchdb/local.ini",
				path + "/couchdb/etc/couchdb/android.default.ini"};

		ArrayList<String> args = new ArrayList<String>(Arrays.asList(plainArgs));
		for (String iniPath : CouchbaseMobile.customIniFiles) {
			args.add(iniPath);
		}

		args.add(path + "/couchdb/etc/couchdb/overrides.ini -s couch");

		String shell = "/system/bin/sh";
		String couchbin = join(args, " ");

		int[] pidbuffer = new int[1];
		final FileDescriptor fd = Exec.createSubprocess(shell, cmd, couchbin, pidbuffer);
		pid = pidbuffer[0];
		out = new PrintStream(new FileOutputStream(fd), true);
		in = new BufferedReader(new InputStreamReader(new FileInputStream(fd)));

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (fd.valid()) {
						String line = in.readLine();
						Log.v(CouchbaseMobile.TAG, line);
						if (line.contains("has started on")) {
							couchbaseStarted = true;
							url = new URL(matchURLs(line).get(0));
							Message.obtain(mHandler, CouchbaseService.COUCHBASE_STARTED, url)
								.sendToTarget();
						}
					}
				} catch (Exception e) {
					Log.v(CouchbaseMobile.TAG, "Couchbase has stopped unexpectedly");
					Message.obtain(mHandler, CouchbaseService.ERROR, e).sendToTarget();
				}
			}
		}).start();
	}

	/* Stop the Couch process */
	private void stop() {
		try {
			out.close();
			android.os.Process.killProcess(pid);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		couchbaseStarted = false;
	}

	private ArrayList<String> matchURLs(String text) {
		ArrayList<String> links = new ArrayList<String>();
		String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
		Matcher m = Pattern.compile(regex).matcher(text);
		while(m.find()) {
			links.add(m.group());
		}
		return links;
	}

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