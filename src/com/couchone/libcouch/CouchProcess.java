package com.couchone.libcouch;

import java.io.BufferedReader;
import java.io.FileDescriptor;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintStream;
import android.util.Log;

import com.google.ase.Exec;

public class CouchProcess {

	public static String TAG = "CouchDB";

	public CouchService service;

	public static String adminUser = "admin";
	public static String adminPass;

	// TODO: read from config file
	public final String host = "127.0.0.1";
	public final int port = 5984;

	public boolean started = false;
		    
	private Integer pid;
	private PrintStream out;
	private BufferedReader in;
	
	public void start(String binary, String arg1, String arg2) {

		int[] pidbuffer = new int[1];
		final FileDescriptor fd = Exec.createSubprocess(binary, arg1, arg2, pidbuffer);
		pid = pidbuffer[0];
		out = new PrintStream(new FileOutputStream(fd), true);
		in = new BufferedReader(new InputStreamReader(new FileInputStream(fd)));

		new Thread(new Runnable() {
			public void run() {
				try {				
					while (fd.valid()) {					
						String line = in.readLine();
						Log.v(TAG, line);
						if (line.contains("has started on")) {
							started = true;
							service.couchStarted();
						}
					}
				} catch (IOException e) {
					// Closed io from seperate thread
				} catch (Exception e) {
					Log.v(TAG, "CouchDB has stopped unexpectedly");
					e.printStackTrace();
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
		return "http://" + host + ":" + Integer.toString(port) + "/";
	}

}
