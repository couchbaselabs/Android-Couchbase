package com.couchone.libcouch;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.couchone.libcouch.ICouchClient;
import com.couchone.libcouch.ICouchService;

import android.app.NotificationManager;
import android.app.Service;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Binder;
import android.os.IBinder;
import android.os.RemoteException;

public class CouchService extends Service {

	private CouchProcess couch = new CouchProcess();
	
	// A list of couchClients that awaiting notifications of couch starting
	private Map<String, ICouchClient> couchClients = new HashMap<String, ICouchClient>();

	// Contains a mapping of database names to their listeners
	public class dbListeners {
		private Map<String, CouchCtrlListener> databases = new HashMap<String, CouchCtrlListener>();
	}

	// Contains a mapping of package names to database listeners
	private Map<String, dbListeners> listeners = new HashMap<String, dbListeners>();

	private NotificationManager mNM;

	/*
	 * This is called to start the service
	 */
	@Override
	public void onCreate() {
		couch.service = this;
		couch.start("/system/bin/sh", CouchInstaller.couchPath + "/bin/couchdb", "");
	}

	@Override
	public void onDestroy() {
		if (couch.started) { 
			couch.stop();
		}
		couchClients.clear();
		mNM.cancelAll();
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
		public void initCouchDB(ICouchClient cb) throws RemoteException {

			String packageName = packageNameFromUid(Binder.getCallingUid());
			couchClients.put(packageName, cb);

			// Notify the client straight away that couch has started
			if (couch.started == true) {
				couchStarted();
			}
		}

		/*
		 * When a client quits we just cancel any control database listeners
		 * dont actually stop couch, that is done when the last client unbinds
		 */
		@Override
		public void quitCouchDB() throws RemoteException {
			cancelListeners();
		}
	};

	/*
	 * once couch has started we need to notify all waiting clients
	 */
	void couchStarted() throws RemoteException {
		
		for (Entry<String, ICouchClient> entry : couchClients.entrySet()) {
			ICouchClient client = entry.getValue();
			client.couchStarted(couch.host, couch.port);
			couchClients.remove(entry.getKey());
		}
	}

	/*
	 * Control databases have a long polling listener, stop them when an application
	 * quits
	 */
	private void cancelListeners() { 
		String packageName = packageNameFromUid(Binder.getCallingUid());
		if (listeners.containsKey(packageName)) {
			dbListeners tmp = listeners.get(packageName);
			for (Map.Entry<String, CouchCtrlListener> temp : tmp.databases.entrySet()) {
				temp.getValue().cancel();
			}
		}
	}

	private String packageNameFromUid(int uid) {
		PackageManager pm = getPackageManager();
		String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
		return packages[0];
	}
}
