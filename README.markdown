## Mobile Couchbase for Android

Apache CouchDB on Android provides a simple way to sync your application data across devices and provide cloud backup of user data. Unlike other cloud solutions, the data is hosted on the device by Couchbase Mobile, so even when the network is down or slow (airplane, subway, backyard) the application is responsive to users.

What this means for you:

* You can embed the rock solid distributed database, Mobile Couchbase, on your Android device.
* Your Android apps can use Apache CouchDB's well-proven synchronization technology.
* If you <3 CouchApps, you can deploy them as Android apps.

## Join us

There is a [Google Group here for Mobile Couchbase](https://groups.google.com/group/mobile-couchbase). Let's talk about how to optimize the Erlang build, what the best Java APIs are for CouchDB, how to take advantage of replication on mobile devices. It'll be fun.

## Bug Tracker

There is a bug tracker available at [http://www.couchbase.org/issues/browse/CBMA](http://www.couchbase.org/issues/browse/CBMA)

## Getting Started

If you have questions or get stuck or just want to say hi, email <mobile@couchbase.com> and let us know that you're interested in Couchbase on mobile.

These instructions require you to have installed Eclipse and the Android SDK, the [Android developer website](http://developer.android.com/sdk/installing.html) has instructions.

## Installing Couchbase into your Application

NOTE: You do not need the source of this repository to use Mobile Couchbase in your Android application.

1.  Create a new Android project or select an existing project

2.  Download the Android-Couchbase.zip
  - https://github.com/downloads/couchbase/Android-Couchbase/Android-Couchbase.zip

3.  Extract Android-Couchbase.zip, you will see 3 files
  - Couchbase.zip
  - couchbase.xml
  - README.txt

4.  Place Couchbase.zip and couchbase.xml into the top-level of your project

5.  Right-click on couchbase.xml and select Run As > Ant Build

6.  Refresh your project

## Starting Couchbase

Now that your project supports Couchbase, starting Cocuhbase is accomplished by adding a few things to your application's Main Activity.

1.  Create an instance of ICouchbaseDelegate, you can implement these methods to respond to Couchbase events
<pre>    
    private final ICouchbaseDelegate mCallback = new ICouchbaseDelegate.Stub() {
        @Override
        public void couchbaseStarted(String host, int port) {}
    
        @Override
        public void installing(int completed, int total) {}
    
        @Override
        public void exit(String error) {}
    };
</pre>

2.  Declare a ServiceConnection instance to keep a reference to the Couchbase service
<pre>
    private ServiceConnection couchServiceConnection;
</pre>

3.  Add a method to start Couchbase
<pre>
        public void startCouchbase() {
                CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), mCallback);
                couchServiceConnection = couch.startCouchbase();
        }
</pre>

4.  Call the startCouchbase method from the appropriate Activity lifecycle methods.  For many applications the onCreate method is appropriate
<pre>    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    
        ...
    
        startCouchbase();
    }
</pre>    

## Examples

For examples please look at https://github.com/couchbase/Android-EmptyApp

## Build information

To build the Android-Couchbase project from source run the following ant command:

    ant -Dsdk.dir=/path/to/android/sdk dist

Replacing the value for sdk.dir with the path to the Android SDK on the build server.

The current build of Android Couchbase embed the CouchDB binaries. There is information on how to build these binaries on the [SourceBuild](https://github.com/couchbase/Android-Couchbase-SourceBuild) project.

## License

Portions under Apache, Erlang, and other licenses.

The overall package is released under the Apache license, 2.0.

Copyright 2011, Couchbase, Inc.
