package com.couchone.libcouch;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

import com.couchone.libcouch.ICouchClient;
import com.couchone.libcouch.ICouchService;

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
		 */
		@Override
		public void quitCouchDB() throws RemoteException {
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

	private String packageNameFromUid(int uid) {
		PackageManager pm = getPackageManager();
		String[] packages = pm.getPackagesForUid(Binder.getCallingUid());
		return packages[0];
	}
}
