package com.couchone.libcouch;

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

	public final static int INSTALLING = 3;
	public final static int INITIALIZING = 4;

	private int status;
	private ICouchClient client;

	private final Handler mHandler = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			try {
				switch (msg.what) {
				case ERROR:
					client.progress(ERROR, 0, 0);
					break;
				case PROGRESS:
					client.progress(status, msg.arg1, msg.arg2);
					break;
				case COMPLETE:
					if (status == INSTALLING) {
						initCouch();
					} else {
						couch.start("/system/bin/sh", CouchInstaller.couchPath() + "/bin/couchdb", "");
					}
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
			} else if (!CouchInitializer.isEnvironmentInitialized()) {
				initCouch();
			} else {
				if (couch.started == true) {
					couchStarted();
				} else {
					couch.start("/system/bin/sh", CouchInstaller.couchPath() + "/bin/couchdb", "");
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

	void installCouch(final String url, final String pkg) {
		status = INSTALLING;
		new Thread() {
			@Override
			public void run() {
				try {
					CouchInstaller.doInstall(url, pkg, mHandler);
				} catch (Exception e) {
					try {
						client.progress(ERROR, 0, 0);
					} catch (RemoteException e1) {
						e1.printStackTrace();
					}
					e.printStackTrace();
				}
			}
		}.start();
	}

	void initCouch() {
		status = INITIALIZING;
		new Thread() {
			@Override
			public void run() {
				try {
					CouchInitializer.initializeEnvironment(mHandler);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
		}.start();
	}
}
