##  Prerequisites

These instructions require you to have installed Eclipse and the Android SDK, http://developer.android.com/sdk/installing.html

##  Installation Instructions

1.  Create a new Android project or select an existing project

2.  Copy the Couchbase.zip and couchbase.xml files into the top-level of the project:

3.  Right-click on couchbase.xml and select Run As > Ant Build

4.  Refresh your project

## Starting Couchbase

Now that your project supports Couchbase, starting Cocuhbase is accomplished by adding a few things to your application's Main Activity.

1.  Create an instance of ICouchbaseDelegate, you can implement these methods to respond to Couchbase events

    private final ICouchbaseDelegate mDelegate = new ICouchbaseDelegate() {
        @Override
        public void couchbaseStarted(String host, int port) {}
    
        @Override
        public void exit(String error) {}
    };

2.  Declare a ServiceConnection instance to keep a reference to the Couchbase service

    private ServiceConnection couchServiceConnection;

3.  Add a method to start Couchbase

        public void startCouchbase() {
                CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), mCallback);
                couchServiceConnection = couch.startCouchbase();
        }

4.  Call the startCouchbase method from the appropriate Activity lifecycle methods.  For many applications the onCreate method is appropriate
    
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ...

        startCouchbase();
    }

## Broadcast Intents

A Broadcast Receiver registered to listen for the right events can now be used instead of a delegate.

1.  Optionally, use the CouchbaseMobile constructor without the delegate parameter

    CouchbaseMobile couch = new CouchbaseMobile(getBaseContext());

2.  Declare a Broadcast Receiver

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {

        @Override
        public void onReceive(Context context, Intent intent) {
            if(CouchbaseStarted.ACTION.equals(intent.getAction())) {
                String host = CouchbaseStarted.getHost(intent);
                int port = CouchbaseStarted.getPort(intent);
            }
            else if(CouchbaseError.ACTION.equals(intent.getAction())) {
                String message = CouchbaseError.getMessage(intent);
            }
        }
    };

3.  Register the receiver to listen for the appropriate events

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        ...

        registerReceiver(mReceiver, new IntentFilter(CouchbaseStarted.ACTION));
        registerReceiver(mReceiver, new IntentFilter(CouchbaseError.ACTION));
    }

## Examples

For examples please look at:

* https://github.com/couchbase/Android-EmptyApp
* https://github.com/daleharvey/Android-MobileFuton
* https://github.com/couchbaselabs/AndroidGrocerySync

## Manual Installation

In some environments it may not be possible to use the couchbase.xml ant script installer.  Couchbase can be installed manually using the following steps.

1.  Unzip the Couchbase.zip archive.  This will produce another zip file named overlay.zip.
2.  Remove any existing couchbase*.jar file from <project root>/libs
3.  Extract the contents of the overlay.zip file into your project.  This will place all assets and libraries in the correct location within the structure of your project.

    cd <project root>
    unzip /<path to>/overlay.zip

4.  Update the project's AndroidManifest.xml to declare the Couchbase service and request the required permissions.

    Within the "application" section add:

    <service android:name="com.couchbase.android.CouchbaseService" android:enabled="true" android:exported="false"/>

    Within the "manifest" section add:

    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"/>
    <uses-permission android:name="android.permission.INTERNET"/>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"/>

## Requirements

- Android 2.1 or newer
- Eclipse 3.6.2 or newer (if using Eclipse)
- Ant 1.7.1 or newer (if using Ant outside of Eclipse)
- When testing in the emulator, be sure to create an SD Card with sufficient space for your databases
- If using views in the emulator, create an AVD with CPU/ABI set to armeabi-v7a

## Join us

There is a Google Group here for Mobile Couchbase at https://groups.google.com/group/mobile-couchbase. Let's talk about how to optimize the Erlang build, what the best Java APIs are for CouchDB, how to take advantage of replication on mobile devices. It'll be fun.

## Develop

Interested in the Android-Couchbase internals?  Check out the GitHub repository https://github.com/couchbase/Android-Couchbase
