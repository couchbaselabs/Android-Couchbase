package com.couchbase.libcouch;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.google.ase.Exec;

public class CouchProcess {

	public static String TAG = "CouchDB";

	public CouchService service;

	public URL url;

	public boolean started = false;

	private Integer pid;
	private PrintStream out;
	private BufferedReader in;

	public void start(String binary, String arg1, String arg2, final Handler handler) {

		int[] pidbuffer = new int[1];
		final FileDescriptor fd = Exec.createSubprocess(binary, arg1, arg2, pidbuffer);
		pid = pidbuffer[0];
		out = new PrintStream(new FileOutputStream(fd), true);
		in = new BufferedReader(new InputStreamReader(new FileInputStream(fd)));

		new Thread(new Runnable() {
			@Override
			public void run() {
				try {
					while (fd.valid()) {
						String line = in.readLine();
						Log.v(TAG, line);
						if (line.contains("has started on")) {
							started = true;
							url = new URL(matchURLs(line).get(0));
							Message.obtain(handler, CouchService.COUCH_STARTED, url)
								.sendToTarget();
						}
					}
				} catch (Exception e) {
					Log.v(TAG, "CouchDB has stopped unexpectedly");
					Message.obtain(handler, CouchService.ERROR, e).sendToTarget();
				}
			}
		}).start();
	}

	public void stop() {
		try {
			out.close();
			android.os.Process.killProcess(pid);
			in.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
		started = false;
	}

	public String url() {
		return url.toString();
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
}
