package com.couchone.libcouch;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

public class CouchInitializer
{
    private final static String t = "CouchInitializer";

    private static final String COUCHDIR = "couchdb";
    private static final String ERLANGDIR = "erlang";

    private static final String DIRECTORY = "d";
    private static final String LINK = "l";
    private static final String FILE = "f";

    // The private data directory to initialize into
    private static String destinationPath() {
    	return CouchInstaller.dataPath();
    }

    public static boolean isEnvironmentInitialized()
    {
        return new File(destinationPath(), COUCHDIR).isDirectory();
    }

    public static void initializeEnvironment(Handler handler)
    {
        List<String> index = new ArrayList<String>();

        // This method must have the index of installed files as prepared by CouchInstaller
        if (new File(CouchInstaller.indexFile()).exists() == false) {
            Log.e(t, "unable to initialize data directory: index of installed files is missing");
            return;
        }

        // Remove pre-existing files (from upgrades, broken installs, etc)
        if (new File(destinationPath(), COUCHDIR).isDirectory()) {
            Log.v(t, "purging " + COUCHDIR + " data directory...");
            CouchInstaller.deleteDirectory(new File(destinationPath(), COUCHDIR));
        }

        // Remove pre-existing files (from upgrades, broken installs, etc)
        if (new File(destinationPath(), ERLANGDIR).isDirectory()) {
            Log.v(t, "purging " + ERLANGDIR + " data directory...");
            CouchInstaller.deleteDirectory(new File(destinationPath(), ERLANGDIR));
        }

        Log.i(t, "initializing data directory for CouchDB and Erlang/OTP");
        Log.d(t, "destination path is " + destinationPath());

        /*
         * Go through each of the files listed in the index of installed files and take appropriate
         * action depending on the type of file listed.  The purpose of this loop is to copy or link
         * files into the /data/data hierarchy where executable files can be used (and where the
         * compiled version of Couch/Erlang expects to find them).
         */
        try {
            index = org.apache.commons.io.FileUtils.readLines(new File(CouchInstaller.indexFile()));
            Iterator<String> entries = index.iterator();

            float entriesProcessed = 0;

            while (entries.hasNext()) {
                String entry = entries.next();

                initializeEntry(entry);

                if (handler != null) {
                    Message progress = new Message();
                    progress.arg1 = (int) ++entriesProcessed;
                    progress.arg2 = index.size();
                    progress.what = CouchService.PROGRESS;
                    handler.sendMessage(progress);
                }
            }

            // Close progress bar
            Message progress = new Message();
            progress.arg1 = 1;                  // Our way of telling the handler not to restart the activity
            progress.arg2 = 0;
            progress.what = CouchService.COMPLETE;
            handler.sendMessage(progress);
        } catch (IOException e) {
            // TODO Auto-generated catch block
            Log.e(t, "unable to read index file: " + e.toString());
            e.printStackTrace();
            return;
        }
    }

    /*
     * Figure out what to do with a file listed in the index of installed files
     *
     * The format of an entry in the index is
     * FILE_TYPE FILE_MODE FILE_PATH LINK_TO
     * 0         1         2         3
     */
    private static void initializeEntry(String entry)
    {
        Log.v(t, "initializing " + entry);

        String [] info = entry.split("\\s");
        String neutralPath = info[2].replace(CouchInstaller.externalPath(), "");

        if (info[0].equals(DIRECTORY)) {
            new File(destinationPath(), neutralPath).mkdirs();

            try {
                Runtime.getRuntime().exec("chmod 755 " + destinationPath() + neutralPath);
            } catch (IOException e) {
                Log.e(t, "failed to chmod 755 " + e.toString());
                e.printStackTrace();
            }
        }

        if (info[0].equals(LINK)) {
            try {
                Runtime.getRuntime().exec("/system/bin/ln -s " + info[3] + " " + destinationPath() + neutralPath);
            } catch (IOException e) {
                Log.e(t, "failed to link " + e.toString());
                e.printStackTrace();
            }
        }

        if (info[0].equals(FILE)) {
            if (info[1].equals("420")) {
                try {
                	Runtime.getRuntime().exec("/system/bin/ln -s " + CouchInstaller.externalPath() + neutralPath + " " + destinationPath() + neutralPath);
                } catch (IOException e) {
                    Log.e(t, "failed to link " + e.toString());
                    e.printStackTrace();
                }
            } else if (info[1].equals("493")) {
                try {
                    org.apache.commons.io.FileUtils.copyFile(new File(CouchInstaller.externalPath(), neutralPath), new File(destinationPath(), neutralPath));
                    Runtime.getRuntime().exec("/system/bin/chmod 755 " + destinationPath() + neutralPath);
                } catch (IOException e) {
                    Log.e(t, "unable to duplicate " + e.toString());
                    e.printStackTrace();
                }
            } else {
                Log.e(t, "unhandled file mode " + info[1]);
            }
        }
    }
}