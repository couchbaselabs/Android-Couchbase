package com.couchbase.libcouch;

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
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;

import com.google.ase.Exec;

public class CouchService extends Service {

	/*
	 * Contants used to communnicate between the Couch Service
	 * and this thread
	 */
	public final static int ERROR = 0;
	public final static int PROGRESS = 1;
	public final static int COMPLETE = 2;
	public final static int COUCH_STARTED = 3;
	public final static int INSTALLING = 4;

	/* The url Couch has started on */
	public URL url;

	/* Has Couch started */
	public boolean couchStarted = false;

	/* This is how we talk to the app that started Couch */
	private ICouchClient client;

	private Integer pid;
	private PrintStream out;
	private BufferedReader in;

	/*
	 * Couch runs in a seperate thread, have the communication run
	 * through our own handler so the app doesnt need to create a new
	 * handler to deal with touching the UI from a different thread
	 */
	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case ERROR:
					Exception e = (Exception) msg.obj;
					StringWriter sw = new StringWriter();
					e.printStackTrace(new PrintWriter(sw));
					String stacktrace = sw.toString();
					if (client != null) {
						client.exit(stacktrace);
					}
					break;
				case PROGRESS:
					client.installing(msg.arg1, msg.arg2);
					break;
				case COMPLETE:
					startCouch();
					break;
				case COUCH_STARTED:
					URL url = (URL) msg.obj;
					client.couchStarted(url.getHost(), url.getPort());
					break;
				}
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
	};

	/*
	 * This is called when the service is destroyed
	 */
	@Override
	public void onDestroy() {
		if (couchStarted) {
			stop();
		}
		client = null;
	}

	/*
	 * This is called called on the initial service binding
	 */
	@Override
	public IBinder onBind(Intent intent) {
		return new CouchServiceImpl();
	}

	/*
	 * implements the callbacks that clients can call into the couchdb service
	 */
	public class CouchServiceImpl extends ICouchService.Stub {

		@Override
		public void startCouchbase(ICouchClient cb, final String pkg) throws RemoteException {
			client = cb;
			if (!CouchInstaller.checkInstalled(pkg)) {
				installCouch(pkg);
			} else {
				if (couchStarted == true) {
					couchStarted();
				} else {
					startCouch();
				}
			}
		}

		/*
		 */
		@Override
		public void stopCouchbase() throws RemoteException {
			stop();
		}
	};

	/* once couch has started we need to notify the waiting client */
	void couchStarted() throws RemoteException {
		client.couchStarted(url.getHost(), url.getPort());
	}

	/* Install Couch in a seperate thread */
	private void installCouch(final String pkg) {
		final CouchService service = this;
		new Thread() {
			@Override
			public void run() {
				try {
					CouchInstaller.doInstall(pkg, mHandler, service);
				} catch (FileNotFoundException e) {
					Message.obtain(mHandler, CouchService.ERROR, e).sendToTarget();
					e.printStackTrace();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}.start();
	}

	/* Start the couch process, run in a seperate thread */
	void startCouch() {

		String path = CouchbaseEmbeddedServer.dataPath();

		String cmd = path + "/couchdb/bin/couchdb_wrapper";

		String[] plainArgs = {
				"+Bd", "-noinput", "sasl", "errlog_type", "all", "+K", "true",
				"-env", "ERL_LIBS", path + "/couchdb/lib/couchdb/erlang/lib", "-couch_ini",
				path + "/couchdb/etc/couchdb/default.ini",
				path + "/couchdb/etc/couchdb/local.ini",
				path + "/couchdb/etc/couchdb/android.default.ini"};

		ArrayList<String> args = new ArrayList<String>(Arrays.asList(plainArgs));
		for (String iniPath : CouchbaseEmbeddedServer.customIniFiles) {
			args.add(iniPath);
		}

		args.add(path + "/couchdb/etc/couchdb/overrides.ini -s couch");

		String shell = "/system/bin/sh";
		String couchbin = join(args, " ");

		Log.v(CouchbaseEmbeddedServer.TAG, "couchbin");
		Log.v(CouchbaseEmbeddedServer.TAG, couchbin);

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
						Log.v(CouchbaseEmbeddedServer.TAG, line);
						if (line.contains("has started on")) {
							couchStarted = true;
							url = new URL(matchURLs(line).get(0));
							Message.obtain(mHandler, CouchService.COUCH_STARTED, url)
								.sendToTarget();
						}
					}
				} catch (Exception e) {
					Log.v(CouchbaseEmbeddedServer.TAG, "CouchDB has stopped unexpectedly");
					Message.obtain(mHandler, CouchService.ERROR, e).sendToTarget();
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
		couchStarted = false;
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
