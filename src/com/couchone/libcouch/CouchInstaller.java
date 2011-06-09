package com.couchone.libcouch;

import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.StatusLine;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;

import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CouchInstaller {
	
	final static String baseUrl = "http://junk.arandomurl.com/";
	public final static String appNamespace = "com.test.couch";
	public final static String dataPath = "/data/data/" + appNamespace;
	public final static String externalPath = Environment.getExternalStorageDirectory() + "/Android/data/" + appNamespace;
	
	public final static String couchPath = externalPath + "/couchdb";
	final static String erlangPath = externalPath + "/erlang";
	
	public final static String indexFile = externalPath + "/installedfiles.index";

	final static String TAG = "CouchDB";
	
	public final static int ERROR = 0;
	public final static int PROGRESS = 1;
	public final static int COMPLETE = 2;

	public static void doInstall(Handler handler) throws IOException {
		
		// WARNING: This deleted any previously installed couchdb data 
		// and binaries stored on the sdcard to keep in line with usual 
		// android app behaviour. However there doesnt look to be a way to protect
		// ourselves from wiping the entire sdcard with a typo, so just be 
		// careful
		File couchDir = new File(couchPath);
		if (couchDir.exists()) {
			deleteDirectory(couchDir);
		}
		
		for(String pkg : packageSet()) {
			if(!(new File(externalPath + "/" + pkg + ".installedfiles")).exists()) {
				installPackage(pkg, handler);
			}	
		}

		Message done = Message.obtain();
		done.what = CouchInstaller.COMPLETE;
		handler.sendMessage(done);
	}

	/* 
	 * This fetches a given package from amazon and tarbombs it to the filsystem
	 */
	private static void installPackage(String pkg, Handler handler)
			throws IOException {
		
		Log.v(TAG, "Installing " + pkg);
		
		HttpClient pkgHttpClient = new DefaultHttpClient();
		HttpGet tgzrequest = new HttpGet(baseUrl + pkg + ".tgz");
		HttpResponse response = pkgHttpClient.execute(tgzrequest);
		ArrayList<String> installedfiles = new ArrayList<String>();
		StatusLine status = response.getStatusLine();
		Log.d(TAG, "Request returned status " + status);
		
		// Later used initialization of /data/data/...
		ArrayList<String> allInstalledFiles = new ArrayList<String>();
		Map<String, Integer> allInstalledFileModes = new HashMap<String, Integer>();
		Map<String, String> allInstalledFileTypes = new HashMap<String, String>();
		Map<String, String> allInstalledLinks = new HashMap<String, String>();
		
		if (status.getStatusCode() == 200) {
			HttpEntity entity = response.getEntity();
			InputStream instream = entity.getContent();
			TarArchiveInputStream tarstream = new TarArchiveInputStream(
					new GZIPInputStream(instream));
			TarArchiveEntry e = null;
			
			int files = 0;
			float filesInArchive = 0;
			float filesUnpacked = 0;
			
			while ((e = tarstream.getNextTarEntry()) != null) {
			    // Obtain count of files in this archive so that we can indicate install progress
			    if (filesInArchive == 0 && e.getName().startsWith("filecount")) {
			        String[] count = e.getName().split("\\.");
			        filesInArchive = Integer.valueOf(count[1]);
			        continue;
			    }
			    
				if (e.isDirectory()) {
					File f = new File(e.getName());
					if (!f.exists() && !new File(e.getName()).mkdir()) { 
						throw new IOException("Unable to create directory: " + e.getName());
					}
					Log.v(TAG, "MKDIR: " + e.getName());
					
					allInstalledFiles.add(e.getName());
					allInstalledFileModes.put(e.getName(), e.getMode());
					allInstalledFileTypes.put(e.getName(), "d");
				} else if (!"".equals(e.getLinkName())) {
					Log.v(TAG, "LINK: " + e.getName() + " -> " + e.getLinkName());
					Runtime.getRuntime().exec(new String[] { "ln", "-s", e.getName(), e.getLinkName() });
					installedfiles.add(e.getName());
					
					allInstalledFiles.add(e.getName());
					allInstalledLinks.put(e.getName(), e.getLinkName());
					allInstalledFileModes.put(e.getName(), e.getMode());
					allInstalledFileTypes.put(e.getName(), "l");
				} else {
					File target = new File(e.getName());
					if(target.getParent() != null) {
						new File(target.getParent()).mkdirs();
					}
					Log.v(TAG, "Extracting " + e.getName());
					IOUtils.copy(tarstream, new FileOutputStream(target));
					installedfiles.add(e.getName());
					
					allInstalledFiles.add(e.getName());
					allInstalledFileModes.put(e.getName(), e.getMode());
					allInstalledFileTypes.put(e.getName(), "f");
				}
				
				// getMode: 420 (644), 493 (755), 509 (775), 511 (link 775)
				//Log.v(TAG, "File mode is " + e.getMode());
				
				//TODO: Set to actual tar perms.
				Runtime.getRuntime().exec("chmod 755 " + e.getName()); 
				
				// This tells the ui how much progress has been made
				files++;
				Message progress = new Message();
				progress.arg1 = (int) Math.round(++filesUnpacked / filesInArchive * 100);
				progress.arg2 = 0;
				progress.what = CouchInstaller.PROGRESS;
				handler.sendMessage(progress);
			}

			tarstream.close();
			instream.close();
			
			FileWriter iLOWriter = new FileWriter(externalPath + "/" + pkg + ".installedfiles");
			for (String file : installedfiles) {
				iLOWriter.write(file+"\n");
			}
			iLOWriter.close();
			for (String file : installedfiles) {
				if(file.endsWith(".postinst.sh")) {
					Runtime.getRuntime().exec("sh " + file);
				}
			}
			
			// Write out full list of all installed files + file modes
			iLOWriter = new FileWriter(indexFile);
			
			for (String file : allInstalledFiles) {
			    iLOWriter.write(
			            allInstalledFileTypes.get(file).toString() + " " + 
			            allInstalledFileModes.get(file).toString() + " " + 
			            file + " " +
			            allInstalledLinks.get(file) + "\n");
			}
			
			iLOWriter.close();
		} else {
			throw new IOException();
		}
	}

	/*
	 * Verifies that CouchDB is installed by checking the package files we 
	 * write on installation + the data directory on the sd card
	 */
	public static boolean checkInstalled() {
				
		for (String pkg : packageSet()) {
			File file = new File(externalPath + "/" + pkg + ".installedfiles");
			if (!file.exists()) {
				return false;
			}
		}
		
		return new File(couchPath).exists();
	}


	/*
	 * List of packages that need to be installed
	 */
	public static List<String> packageSet() {
		ArrayList<String> packages = new ArrayList<String>();
	
		// TODO: Different CPU arch support.
		// TODO: Some kind of sane remote manifest for this (remote updater)
//		packages.add("couch-erl-1.0"); // CouchDB, Erlang, CouchJS
//		packages.add("fixup-1.0"); //Cleanup old mochi, retrigger DNS fix install.
//		packages.add("dns-fix"); //Add inet config to fallback on erlang resolver
//		if (android.os.Build.VERSION.SDK_INT == 7) {
//			packages.add("couch-icu-driver-eclair");
//		} else if (android.os.Build.VERSION.SDK_INT == 8) {
//			packages.add("couch-icu-driver-froyo");
//		} else if (android.os.Build.VERSION.SDK_INT == 9) {	
//			packages.add("couch-icu-driver-gingerbread");
//		} else {
//			throw new RuntimeException("Unsupported Platform");
//		}
		
		if (android.os.Build.VERSION.SDK_INT == 8) {
		    packages.add("release-1");
		} else {
		    throw new RuntimeException("Unsupported Platform");
		}
		
		return packages;
	}
	
	/*
	 * Recursively delete directory
	 */
	public static Boolean deleteDirectory(File dir) {
		if (dir.isDirectory()) {
			String[] children = dir.list();
			for (int i = 0; i < children.length; i++) {
				boolean success = deleteDirectory(new File(dir, children[i]));
				if (!success) {
					return false;
				}
			}
		}
		return dir.delete();
	}
}
