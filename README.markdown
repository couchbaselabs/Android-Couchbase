## Mobile Couchbase for Android

Apache CouchDB on Android provides a simple way to sync your application data across devices and provide cloud backup of user data. Unlike other cloud solutions, the data is hosted on the device by Couchbase Mobile, so even when the network is down or slow (airplane, subway, backyard) the application is responsive to users.

What this means for you:

* You can embed the rock solid distributed database, Mobile Couchbase, on your Android device.
* Your Android apps can use Apache CouchDB's well-proven synchronization technology.
* If you <3 CouchApps, you can deploy them as Android apps.

### Beta Release

If you just want to get started, jump to **Getting Started**.

The biggest thing we need help with is size optimization - currently a Release build adds about 7 MB to your application. We are targeting 5 MB for our initial round of optimizations. It can definitely go lower but that work might take longer.

## Join us

There is a [Google Group here for Mobile Couchbase](https://groups.google.com/group/mobile-couchbase). Let's talk about how to optimize the Erlang build, what the best Java APIs are for CouchDB, how to take advantage of replication on mobile devices. It'll be fun.


## Getting Started

These instructions assume you are familiar with how to make an Android app because you've done it a lot already.

If you have questions or get stuck or just want to say hi, email <mobile@couchbase.com> and let us know that you're interested in Couchbase on mobile.

These instructions require you to have installed Eclipse and the Android SDK, the [Android developer website](http://developer.android.com/sdk/installing.html) has instructions.

### Get the main repository

    git clone git://github.com/couchbaselabs/Android-Couchbase.git

### Open Android-Couchbase in eclipse and check the "Is Library" checkbox is Properties > Android

### Running the demo app

## Starting a new Project

### Create a new Project

right click on your project > Properties > Android and add libcouch in the libraries panel

right click on your project > Properties > Java Build Path > Add JARs and under LibCouch/lib add the commons-compress-1.0.jar and commons-io-2.0.1.jar

Open AndroidManifest in your project and add

    <service
        android:name="com.couchone.libcouch.CouchService"
        android:enabled="true"
        android:exported="false"
        android:process=":remote"></service>

inside your <application> tag and

    <uses-permission android:name="android.permission.INTERNET"></uses-permission>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"></uses-permission>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>

inside the <manifest> tag

inside your activity, the core part of the code needed to interact with couch is

    private ICouchService couchService;
    private boolean couchStarted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        attemptLaunch();
    }

    @Override
    public void onRestart() {
        super.onRestart();
        attemptLaunch();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (couchService != null) {
            try {
                unbindService(mConnection);
            } catch (IllegalArgumentException e) {
                // race condition between activity being destroyed and
	        // service telling us it lost connection
            }
        }
    }

    /*
     * Checks to see if Couch is fully installed, if not prompt to complete
     * installation otherwise start the couchdb service
     */
    private void attemptLaunch() {
        if (!couchStarted) {
            startCouch();
        }
    }

    void startCouch() {
        CouchInstaller.appNamespace = getApplication().getPackageName();
        bindService(new Intent(this, CouchService.class), mConnection, Context.BIND_AUTO_CREATE);
    }

    /*
     * This holds the connection to the CouchDB Service
     */
    private final ServiceConnection mConnection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName className, IBinder service) {
            try {
                couchService = ICouchService.Stub.asInterface(service);
                couchService.initCouchDB(mCallback, "http://junk.arandomurl.com/", "release-1");
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void onServiceDisconnected(ComponentName className) {
            couchService = null;
        }
    };

    /*
     * Implement the callbacks that allow CouchDB to talk to this app
    */
    private final ICouchClient mCallback = new ICouchClient.Stub() {
        @Override
        public void couchStarted(String host, int port) throws RemoteException {
            // CouchDB has started
        }

        @Override
        public void progress(int status, int completed, int total) throws RemoteException {
            // Received a progress message about how far along Couch is done installing
        }
    };


## License

Portions under Apache, Erlang, and other licenses.

The overall package is released under the Apache license, 2.0.

Copyright 2011, Couchbase, Inc.
