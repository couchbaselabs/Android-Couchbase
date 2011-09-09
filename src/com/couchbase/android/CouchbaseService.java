package com.couchbase.android;

import java.io.BufferedReader;
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
	private URL url;

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
	 * Couchbase process
	 */
	private Process process;

	/**
	 * Mapped to Couchbase process stdin
	 */
	private PrintStream out;

	/**
	 * Mapped to Couchbase process stdout
	 */
	private BufferedReader in;

	/**
	 * Mapped to Couchbase process stderr
	 */
	private BufferedReader err;

	/**
	 * Thread responsible for installing Couchbase
	 */
	private CouchbaseInstaller couchbaseInstallThread;

	/**
	 * Thread responsible for communicating with Couchbase process
	 */
	private Thread couchbaseRunThread;

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
				startCouchbaseService();
				break;
			case COUCHBASE_STARTED:
				URL url = (URL) msg.obj;
				if (couchbaseDelegate != null) {
					couchbaseDelegate.couchbaseStarted(url.getHost(), url.getPort());
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
		stop();
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

		String cmd = path + "/couchdb/bin/couchdb_wrapper";

		String[] plainArgs = {
				"+Bd", "-noinput", "sasl", "errlog_type", "all", "+K", "true",
				"-env", "ERL_LIBS", path + "/couchdb/lib/couchdb/erlang/lib", "-couch_ini",
				path + "/couchdb/etc/couchdb/default.ini",
				path + "/couchdb/etc/couchdb/local.ini",
				path + "/couchdb/etc/couchdb/android.default.ini"};

		//build up the command as an array of strings
		ArrayList<String> command = new ArrayList<String>();
		command.add(shell);
		command.add(cmd);
		command.addAll(Arrays.asList(plainArgs));
		for (String iniPath : CouchbaseMobile.getCustomIniFiles()) {
			command.add(iniPath);
		}
		command.add(path + "/couchdb/etc/couchdb/overrides.ini -s couch");

		ProcessBuilder pb = new ProcessBuilder();
		pb.command(command);

		try {
			//start the process
			process = pb.start();

			//connect the streams
			in = new BufferedReader(new InputStreamReader(process.getInputStream()), 8192);
			out = new PrintStream(process.getOutputStream());
			err = new BufferedReader(new InputStreamReader(process.getErrorStream()));

			couchbaseRunThread = new Thread(new Runnable() {
				@Override
				public void run() {
					try {
						String line;
						while ((line = in.readLine()) != null) {
							Log.v(CouchbaseMobile.TAG, line);
							if (line.contains("has started on")) {
								couchbaseStarted = true;
								url = new URL(matchURLs(line).get(0));
								Message.obtain(mHandler, CouchbaseService.COUCHBASE_STARTED, url)
									.sendToTarget();
							}
						}
					} catch (Exception e) {
						Log.v(CouchbaseMobile.TAG, "Couchbase has stopped unexpectedly", e);
						Message.obtain(mHandler, CouchbaseService.ERROR, e).sendToTarget();
						couchbaseStopping = true;
					}
					finally {
						if(!couchbaseStopping) {
							//detect an unplanned shutdown that was not caught by exception
							Log.v(CouchbaseMobile.TAG, "Couchbase has stopped unexpectedly");
							Message.obtain(mHandler, CouchbaseService.ERROR, "Couchbase has stopped unexpectedly").sendToTarget();
						}
						try {
							out.close();
							in.close();
							err.close();
						} catch (IOException e) {
							Log.v(CouchbaseMobile.TAG, "Error closing streams", e);
						}
						finally {
							couchbaseStarted = false;
							stopSelf();
						}
					}
				}
			});
			couchbaseRunThread.start();


		} catch (IOException ioe) {
			Log.v(CouchbaseMobile.TAG, "Failed to start Couchbase process", ioe);
			Message.obtain(mHandler, CouchbaseService.ERROR, ioe).sendToTarget();
		}
	}

	/**
	 * Stop the Couchbase process
	 */
    private void stop() {
        if(!couchbaseStopping) {
                couchbaseStopping = true;

                try {
                        //if installation is running, cancel it and wait for it to finish
                        if(couchbaseInstallThread != null) {
                                couchbaseInstallThread.cancelInstallation();
                                couchbaseInstallThread.join();
                        }

                        //if couchbase process is running kill the process
                        if(process != null) {
                                process.destroy();
                                process = null;
                        }

                        //now wait for the thread to finish
                        if(couchbaseRunThread != null) {
                                couchbaseRunThread.join();
                                couchbaseRunThread = null;
                        }
                } catch (InterruptedException e) {
                        Log.v(CouchbaseMobile.TAG, "Interrupted while waiting for threads to die");
                }
                finally {
                        couchbaseStopping = false;
                }
        }
}

	/**
	 * Utility function to parse a string of text for URLs
	 *
	 * @param text the input text to search
	 * @return array of strings that are URLs
	 */
	private ArrayList<String> matchURLs(String text) {
		ArrayList<String> links = new ArrayList<String>();
		String regex = "\\(?\\b(http://|www[.])[-A-Za-z0-9+&@#/%?=~_()|!:,.;]*[-A-Za-z0-9+&@#/%=~_()|]";
		Matcher m = Pattern.compile(regex).matcher(text);
		while(m.find()) {
			links.add(m.group());
		}
		return links;
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