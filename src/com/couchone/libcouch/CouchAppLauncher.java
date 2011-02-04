package com.couchone.libcouch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

import org.json.JSONException;

import com.couchone.libcouch.AndCouch;

import android.app.Activity;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;
import android.os.RemoteException;
import android.util.Log;
import android.view.KeyEvent;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.Window;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.TextView;

public class CouchAppLauncher extends Activity {

	// Put the databases you want to be created here, if you have
	// a design doc you want to be created then place it in the
	// assets folder eg:
	// private String[] bootstrapDatabases = {"couchnotes", "noteshistory"};
	public String[] bootstrapDatabases = {};

	// If you want to instantiate replication from your application
	// you will need a command database eg:
	// private String[] requiresCommandDatabase = {"couchnotes"};
	public String[] requiresCommandDatabase = {};

	// If you name a database here, its _design/db/index.html
	// will be launched once the databases have been initialised eg:
	// private String appToLaunch = "couchnotes";
	public String appToLaunch = null;
	
	// This contains a mapping of database tags to their
	// actual database names
	private Map<String, String> tagToDbName = new HashMap<String, String>();

	private int initialisedDatabases = 0;

	public final static String COUCHDB_MARKET = "market://details?id=com.couchone.couchdb";
	public final static String TAG = "CouchApp";

	private static final int COUCH_STARTED = 1;
	private static final int DATABASES_INITIALISED = 2;

	// Store the main details to communicate with CouchDB
	public String couchHost;
	public int couchPort;

	public String adminUser = null;
	public String adminPass = null; //readOrGeneratePass("default");

	private WebView webView = null;
	private ICouchService couchService = null;
	private Boolean couchStarted = false;

	/*
	 * This is the entry point of your application, attempt to launch CouchDB
	 * first
	 */
	@Override
	public void onCreate(Bundle savedInstanceState) {
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		super.onCreate(savedInstanceState);
		attemptLaunch();
	};

	/*
	 * This will be called when app comes back from the background, in the case
	 * that CouchDB had to be installed prior to launching, this will attempt to
	 * relaunch it.
	 */
	@Override
	public void onResume() {
		super.onResume();
		if (!couchStarted) {
			attemptLaunch();
		}
	}

	/*
	 * When the app is killed we unbind from CouchDB, so it can stop itself if
	 * not used
	 */
	@Override
	protected void onDestroy() {
		super.onDestroy();

		if (webView != null) {
			webView.destroy();
		}

		if (couchService != null) {
			try {
				couchService.quitCouchDB();
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}
		unbindService(couchServiceConn);
	}

	/*
	 * Attempt to launch the CouchDB service, if CouchDB starts then we will be
	 * notified in the couchClient callback stubs, if not we assume CouchDB is
	 * not started, show a notification screen that then opens the market so the
	 * user can install.
	 */
	public void attemptLaunch() {

		Intent intent = new Intent(ICouchService.class.getName());
		Boolean canStart = bindService(intent, couchServiceConn,
				Context.BIND_AUTO_CREATE);

		if (!canStart) {

			setContentView(R.layout.install_couchdb);

			TextView label = (TextView) findViewById(R.id.install_couchdb_text);
			Button btn = (Button) this.findViewById(R.id.install_couchdb_btn);

			String text = getString(R.string.app_name)
					+ " requires Apache CouchDB to be installed.";
			label.setText(text);

			// Launching the market will fail on emulators
			btn.setOnClickListener(new OnClickListener() {
				public void onClick(View v) {
					launchMarket();
					finish();
				}
			});
		}
	}

	/*
	 * This implements the API that is specified in ICouchClient.aidl, these are
	 * the functions that the CouchDBService will call in your application.
	 */
	private ICouchClient couchClient = new ICouchClient.Stub() {

		@Override
		public void couchStarted(String host, int port) throws RemoteException {
			couchStarted = true;
			couchHost = host;
			couchPort = port;
			localClient.sendMessage(localClient.obtainMessage(COUCH_STARTED));
			initDatabases();
		}

		@Override
		public void databaseCreated(String db, String name, String pass,
				String tag) throws RemoteException {

			initialisedDatabases += 1;
			tagToDbName.put(tag, db);

			// All databases use the same user credentials
			adminUser = name;
			adminPass = pass;

			ensureDesignDoc(db, tag);
			initDatabases();
		}
	};

	/*
	 * This handles the connection to the CouchService
	 */
	private ServiceConnection couchServiceConn = new ServiceConnection() {

		public void onServiceConnected(ComponentName className, IBinder service) {
			couchService = ICouchService.Stub.asInterface(service);
			try {
				couchService.initCouchDB(couchClient);
			} catch (RemoteException e) {
				e.printStackTrace();
			}
		}

		public void onServiceDisconnected(ComponentName className) {
			finish();
			couchService = null;
		}
	};

	/*
	 * The communication with the CouchService is done within a seperate thread,
	 * if we want to do anything to the UI we need to get back into the main
	 * thread, this handler recieves messages from the comms thread
	 */
	private Handler localClient = new Handler() {
		@Override
		public void handleMessage(Message msg) {
			switch (msg.what) {
			// If you want to do something when couch has started, do
			// it here
			case COUCH_STARTED:
				break;
			// This gets sent when all the boostrapDatabases
			// have been created
			case DATABASES_INITIALISED:
				launchWebview();
				break;
			}
		}
	};

	/*
	 * This will loop through "dbfiles" in the assets folder and initialise a db
	 * for each JSON file, db's that do not exist will be created, databases are
	 * identified by their tag although that is not their actual name.
	 */
	private void initDatabases() throws RemoteException {

		if (initialisedDatabases == bootstrapDatabases.length) {
			localClient.sendMessage(localClient
					.obtainMessage(DATABASES_INITIALISED));
			return;
		}

		String dbTag = bootstrapDatabases[initialisedDatabases];
		boolean hasCmdDb = inArray(requiresCommandDatabase, dbTag);
		couchService.initDatabase(couchClient, dbTag, adminPass, hasCmdDb);
	};

	/*
	 * Will check for the existence of a design doc and if it doesnt exist,
	 * upload the json found at dataPath to create it
	 */
	private void ensureDesignDoc(String dbName, String dbTag) {

		try {
			String data = readAsset(dbTag + ".json");
			String ddocUrl = couchUrl() + dbName + "/_design/" + dbTag;

			// Check to see if a design doc already exists
			String auth = Base64Coder.encodeString(adminUser + ":" + adminPass);
			String[][] headers = { { "Authorization", "Basic " + auth } };
			
			AndCouch req = AndCouch.get(ddocUrl, headers);

			if (req.status == 404) {
				AndCouch.put(ddocUrl, data, headers);
			}

		} catch (IOException e) {
			e.printStackTrace();
			// There is no design doc to load
		} catch (JSONException e) {
			e.printStackTrace();
		}
	};

	/*
	 * The appToLaunch member should be set to a database whos index.html is
	 * launched here, if none is set then display a notice.
	 */
	private void launchWebview() {
		if (appToLaunch != null) {
			String dbName = tagToDbName.get(appToLaunch);
			launchUrl(couchUrl() + dbName + "/_design/" + appToLaunch
					+ "/index.html");
		} else {
			setContentView(R.layout.missing_couchapp);
		}
	};

	/*
	 * Create a WebView with the sensible defaults, and load a url within it
	 */
	private void launchUrl(String url) {
		webView = new WebView(this);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new CustomWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDomStorageEnabled(true);
		webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		webView.setHttpAuthUsernamePassword("127.0.0.1", "administrator",
				adminUser, adminPass);
		webView.requestFocusFromTouch();
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		setContentView(webView);
		webView.loadUrl(url);
	};

	/*
	 * If a user pressed the physical back button then make the webview go back
	 * a page if the webview cant go back the default action applied (will
	 * usually close the app)
	 */
	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if ((keyCode == KeyEvent.KEYCODE_BACK) && webView != null
				&& webView.canGoBack()) {
			webView.goBack();
			return true;
		}
		return super.onKeyDown(keyCode, event);
	};

	/*
	 * The custom webview client ensures that when users click links, it opens
	 * in the current webview and doesnt open the browser
	 */
	private class CustomWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			String[] up = view.getHttpAuthUsernamePassword(host, realm);
			handler.proceed(up[0], up[1]);
		}
	}

	private String couchUrl() {
		return "http://" + couchHost + ":" + Integer.toString(couchPort) + "/";
	}

	private void launchMarket() {
		startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(COUCHDB_MARKET)));
	}

	private String readAsset(String path) throws IOException {
		InputStream is = getAssets().open(path);
		int size = is.available();
		byte[] buffer = new byte[size];
		is.read(buffer);
		is.close();
		return new String(buffer);
	}

	private boolean inArray(String[] haystack, String needle) {
		for (int i = 0; i < haystack.length; i++) {
			if (haystack[0].equals(needle)) {
				return true;
			}
		}
		return false;
	}
	
	public String readOrGeneratePass(String user) {
		return readOrGeneratePass(user, generatePassword(8));
	}

	public String readOrGeneratePass(String username, String pass) {
		String passFile =  Environment.getExternalStorageDirectory() + "/couch/" + username + ".passwd";
		File f = new File(passFile);
		if (!f.exists()) {
			writeFile(passFile, username + ":" + pass);
			return pass;
		} else {
			return readFile(passFile).split(":")[1];
		}
	}

	public String generatePassword(int length) {
		String charset = "!0123456789abcdefghijklmnopqrstuvwxyz";
		Random rand = new Random(System.currentTimeMillis());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int pos = rand.nextInt(charset.length());
			sb.append(charset.charAt(pos));
		}
		return sb.toString();
	}

	private String readFile(String filePath) {
		String contents = "";
		try {
			File file = new File(filePath);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			contents = reader.readLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contents;
	};

	private void writeFile(String filePath, String data) {
		try {
			FileWriter writer = new FileWriter(filePath);
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}

}