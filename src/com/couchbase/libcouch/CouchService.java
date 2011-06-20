package com.couchbase.libcouch;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import android.app.Service;
import android.content.Intent;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;

public class CouchService extends Service {

	private final CouchProcess couch = new CouchProcess();

	public final static int ERROR = 0;
	public final static int PROGRESS = 1;
	public final static int COMPLETE = 2;
	public final static int DOWNLOAD = 7;
	public final static int COUCH_STARTED = 5;

	public final static int INSTALLING = 3;
	public final static int INITIALIZING = 4;

	private ICouchClient client;

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
	 * This is called to start the service
	 */
	@Override
	public void onCreate() {
		CouchInstaller.appNamespace = this.getApplication().getPackageName();
		couch.service = this;
	}

	@Override
	public void onDestroy() {
		if (couch.started) {
			couch.stop();
		}
		client = null;
	}

	/*
	 * This is called called on the initial binding
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
		public void initCouchDB(ICouchClient cb, final String url, final String pkg) throws RemoteException {
			client = cb;
			if (!CouchInstaller.checkInstalled(pkg)) {
				installCouch(url, pkg);
			} else {
				if (couch.started == true) {
					couchStarted();
				} else {
					startCouch();
				}
			}
		}

		/*
		 */
		@Override
		public void quitCouchDB() throws RemoteException {
		}
	};

	/*
	 * once couch has started we need to notify the waiting client
	 */
	void couchStarted() throws RemoteException {
		client.couchStarted(couch.url.getHost(), couch.url.getPort());
	}

	void startCouch() {
		couch.start("/system/bin/sh", CouchInstaller.dataPath() + "/couchdb/bin/couchdb", "", mHandler);
	}

	void installCouch(final String url, final String pkg) {
		final CouchService service = this;
		new Thread() {
			@Override
			public void run() {
				try {
					CouchInstaller.doInstall(url, pkg, mHandler, service);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
