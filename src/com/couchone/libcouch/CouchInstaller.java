package com.couchone.libcouch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
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

	public static String appNamespace;

	final static String TAG = "CouchDB";

	public static String dataPath() {
		return "/data/data/" + appNamespace;
	}
	public static String externalPath() {
		return Environment.getExternalStorageDirectory() + "/Android/data/" + appNamespace;
	}
	public static String erlangPath() {
		return externalPath() + "/erlang";
	}
	public static String couchPath() {
		return externalPath() + "/couchdb";
	}
	public static String indexFile() {
		return externalPath() + "/installedfiles.index";
	}

	public static void doInstall(String url, String pkg, Handler handler)
		throws IOException {

		// WARNING: This deleted any previously installed couchdb data
		// and binaries stored on the sdcard to keep in line with usual
		// android app behaviour. However there doesnt look to be a way to protect
		// ourselves from wiping the entire sdcard with a typo, so just be
		// careful
		File couchDir = new File(couchPath());
		if (couchDir.exists()) {
			deleteDirectory(couchDir);
		}

		if(!(new File(externalPath() + "/" + pkg + ".installedfiles")).exists()) {
			installPackage(url, pkg, handler);
		}

		Message done = Message.obtain();
		done.what = CouchService.COMPLETE;
		handler.sendMessage(done);
	}

	/*
	 * This fetches a given package from amazon and tarbombs it to the filsystem
	 */
	private static void installPackage(String baseUrl, String pkg, Handler handler)
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

				String fullName = externalPath() + "/" + e.getName();
			    // Obtain count of files in this archive so that we can indicate install progress
			    if (filesInArchive == 0 && e.getName().startsWith("filecount")) {
			        String[] count = e.getName().split("\\.");
			        filesInArchive = Integer.valueOf(count[1]);
			        continue;
			    }

				if (e.isDirectory()) {
					File f = new File(fullName);
					if (!f.exists() && !new File(fullName).mkdirs()) {
						throw new IOException("Unable to create directory: " + fullName);
					}
					Log.v(TAG, "MKDIR: " + fullName);

					allInstalledFiles.add(fullName);
					allInstalledFileModes.put(fullName, e.getMode());
					allInstalledFileTypes.put(fullName, "d");
				} else if (!"".equals(e.getLinkName())) {
					Log.v(TAG, "LINK: " + fullName + " -> " + e.getLinkName());
					Runtime.getRuntime().exec(new String[] { "ln", "-s", fullName, e.getLinkName() });
					installedfiles.add(fullName);

					allInstalledFiles.add(fullName);
					allInstalledLinks.put(fullName, e.getLinkName());
					allInstalledFileModes.put(fullName, e.getMode());
					allInstalledFileTypes.put(fullName, "l");
				} else {
					File target = new File(fullName);
					if(target.getParent() != null) {
						new File(target.getParent()).mkdirs();
					}
					Log.v(TAG, "Extracting " + fullName);
					IOUtils.copy(tarstream, new FileOutputStream(target));
					installedfiles.add(fullName);

					allInstalledFiles.add(fullName);
					allInstalledFileModes.put(fullName, e.getMode());
					allInstalledFileTypes.put(fullName, "f");
				}

				// getMode: 420 (644), 493 (755), 509 (775), 511 (link 775)
				//Log.v(TAG, "File mode is " + e.getMode());

				//TODO: Set to actual tar perms.
				Runtime.getRuntime().exec("chmod 755 " + fullName);

				// This tells the ui how much progress has been made
				files++;
				Message progress = new Message();
				progress.arg1 = (int) ++filesUnpacked;
				progress.arg2 = (int) filesInArchive;
				progress.what = CouchService.PROGRESS;
				handler.sendMessage(progress);
			}

			tarstream.close();
			instream.close();

			FileWriter iLOWriter = new FileWriter(externalPath() + "/" + pkg + ".installedfiles");
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
			iLOWriter = new FileWriter(indexFile());

			for (String file : allInstalledFiles) {
			    iLOWriter.write(
			            allInstalledFileTypes.get(file).toString() + " " +
			            allInstalledFileModes.get(file).toString() + " " +
			            file + " " +
			            allInstalledLinks.get(file) + "\n");
			}

			iLOWriter.close();

            String[][] replacements = new String[][]{
            	{"%app_name%", CouchInstaller.appNamespace},
            	{"%sdk_int%", Integer.toString(android.os.Build.VERSION.SDK_INT)}
            };

            replace(CouchInstaller.erlangPath() + "/erts-5.7.5/bin/start", replacements);
            replace(CouchInstaller.erlangPath() + "/erts-5.7.5/bin/erl", replacements);
            replace(CouchInstaller.erlangPath() + "/bin/start", replacements);
            replace(CouchInstaller.erlangPath() + "/bin/erl", replacements);
            replace(CouchInstaller.couchPath() + "/etc/init.d/couchdb", replacements);
            replace(CouchInstaller.couchPath() + "/etc/logrotate.d/couchdb", replacements);
            replace(CouchInstaller.couchPath() + "/lib/couchdb/erlang/lib/couch-1.0.2/ebin/couch.app", replacements);
            replace(CouchInstaller.couchPath() + "/lib/couchdb/erlang/lib/couch-1.0.2/priv/lib/couch_icu_driver.la", replacements);
            replace(CouchInstaller.couchPath() + "/bin/couchdb", replacements);
            replace(CouchInstaller.couchPath() + "/bin/couchjs", replacements);
            replace(CouchInstaller.couchPath() + "/bin/couchjs_wrapper", replacements);
            replace(CouchInstaller.couchPath() + "/etc/couchdb/local.ini", replacements);

		} else {
			throw new IOException();
		}
	}

	/*
	 * Verifies that CouchDB is installed by checking the package files we
	 * write on installation + the data directory on the sd card
	 */
	public static boolean checkInstalled(String pkg) {

		File file = new File(externalPath() + "/" + pkg + ".installedfiles");
		if (!file.exists()) {
			return false;
		}

		return new File(couchPath()).exists();
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

    static void replace(String fileName, String[][] replacements) {
        try {
        	File file = new File(fileName);
        	BufferedReader reader = new BufferedReader(new FileReader(file));
        	String line = "", content = "";
        	while((line = reader.readLine()) != null) {
        		content += line + "\n";
        	}

        	for (int i = 0; i < replacements.length; i++) {
        		content = content.replaceAll(replacements[i][0], replacements[i][1]);
        	}
        	reader.close();
        	FileWriter writer = new FileWriter(fileName);
        	writer.write(content);
        	writer.close();
        	Runtime.getRuntime().exec("/system/bin/chmod 755 " + fileName);
        } catch (IOException e) {
        	e.printStackTrace();
        }
    }

}
