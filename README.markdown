## Mobile Couchbase for Android

Apache CouchDB on Android provides a simple way to sync your application data across devices and provide cloud backup of user data. Unlike other cloud solutions, the data is hosted on the device by Couchbase Mobile, so even when the network is down or slow (airplane, subway, backyard) the application is responsive to users.

What this means for you:

* You can embed the rock solid distributed database, Mobile Couchbase, on your Android device.
* Your Android apps can use Apache CouchDB's well-proven synchronization technology.
* If you <3 CouchApps, you can deploy them as Android apps.

### Beta Release

If you just want to get started, jump to **Getting Started**.

The biggest thing we need help with is size optimization - currently a Release build adds about 4 MB to your application (reported as 13MB).

## Join us

There is a [Google Group here for Mobile Couchbase](https://groups.google.com/group/mobile-couchbase). Let's talk about how to optimize the Erlang build, what the best Java APIs are for CouchDB, how to take advantage of replication on mobile devices. It'll be fun.

## Bug Tracker

There is a bug tracker available at [http://www.couchbase.org/issues/browse/CBMA](http://www.couchbase.org/issues/browse/CBMA)

## Getting Started

If you have questions or get stuck or just want to say hi, email <mobile@couchbase.com> and let us know that you're interested in Couchbase on mobile.

These instructions require you to have installed Eclipse and the Android SDK, the [Android developer website](http://developer.android.com/sdk/installing.html) has instructions.

## Building a new application

First you need to download Android-Couchbase

    git clone git://github.com/couchbase/Android-Couchbase.git

And ensure its opened in eclipse (`File -> Import -> Existing Projects`)

Then start a new Android project `File -> New Project -> Android Project` and follow the wizard.

You will need to add some dependancies so your project can see Android-Couchbase. Right click your project and select `Properties -> Android` and on the lower libraries panel press "Add" and select the LibCouch project.

Copy `assets/couchbase-$RELEASE_VERSION.tgz.jpg` from Android-Couchbase/assets into your projects assets directory

back in the properties dialog pick Java Build Path -> Libraries and Add JARS, navigate to LibCouch/lib and choose `commons-io.2.0.1.jar`

Inside your AndroidManifest.xml you will need the following definitions inside the <manifest> tag

    <uses-permission android:name="android.permission.INTERNET"></uses-permission>
    <uses-permission android:name="android.permission.ACCESS_NETWORK_STATE"></uses-permission>
    <uses-permission android:name="android.permission.WRITE_EXTERNAL_STORAGE"></uses-permission>

The following code is what is needed in your application to start Couchbase as a service

as well as

    <service android:name="com.couchbase.libcouch.CouchService"
             android:enabled="true"
             android:exported="false"></service>

inside your <application> tag, then the following code is used inside your application to start Couchbase

    private final ICouchClient mCallback = new ICouchClient.Stub() {
      @Override
      public void couchStarted(String host, int port) {}

      @Override
      public void installing(int completed, int total) {}

      @Override
      public void exit(String error) {}
    };

    String release = "release-0.1";
    ServiceConnection couchServiceConnection = CouchDB.getService(getBaseContext(), null, release, mCallback);

For examples please look at https://github.com/couchbase/Android-EmptyApp

## Build information

The current build of Android Couchbase embed the CouchDB binaries. There is information on how to build these binaries on the [SourceBuild](https://github.com/couchbase/Android-Couchbase-SourceBuild) project.

## License

Portions under Apache, Erlang, and other licenses.

The overall package is released under the Apache license, 2.0.

Copyright 2011, Couchbase, Inc.
