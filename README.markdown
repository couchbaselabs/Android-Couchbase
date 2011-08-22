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

## Downloading

Clone the Android-Couchbase repository

    git clone git://github.com/couchbase/Android-Couchbase.git

And ensure its opened in eclipse (`File -> Import -> Existing Projects`)

## Setting up the Android-Couchbase library

If you have not already done so, you can start a new Android project with `File -> New Project -> Android Project`.

Inside the Couchbase project there is a file `scripts/add-couchbase-to-project.xml`, right click on this file and `Run as -> Ant Build` you will be prompted for 2 paths, these will be stored in the `build.properties`

  `Android SDK`: This is the location you installed the SDK into, the folder will look something like `android-sdk-mac_x86`, (`$ type -p android` may help you find its location, remember to strip the `/tools/android` from that path)

  `Project Path`: This is the path to the Root of your application, Right clicking on your project and selecting `Properties -> Resource` will show you its location.

After you have ran this step you will need to `Refresh` your project (`F5` or `Right Click -> Refresh`)

## Starting Android-Couchbase

Once Android-Couchbase has been setup the following code is used inside your application to start Couchbase

    private final ICouchClient mCallback = new ICouchClient.Stub() {
      @Override
      public void couchStarted(String host, int port) {}

      @Override
      public void installing(int completed, int total) {}

      @Override
      public void exit(String error) {}
    };

    public void startMyApplication() {
        CouchbaseMobile couch = new CouchbaseMobile(getBaseContext(), mCallback);
        couchServiceConnection = couch.startCouchbase();
    }

For examples please look at https://github.com/couchbase/Android-EmptyApp

## Use Existing Application

If you have downloaded EmptyApp or other applications that have already been setup to use Android-Couchbase, you may need to reconfigure some options for your system, open the `build.properties` file and correct the following variables:

  `sdk.dir`: This is the location you installed the SDK into, the folder will look something like `android-sdk-mac_x86`, (`$ type -p android` may help you find its location, remember to strip the `/tools/android` from that path)

  `android.couchbase.dir`: This is the path to Android-Couchbase, Right clicking on the Android-Couchbase project and selecting `Properties -> Resource` will show you its location.

## Build information

The current build of Android Couchbase embed the CouchDB binaries. There is information on how to build these binaries on the [SourceBuild](https://github.com/couchbase/Android-Couchbase-SourceBuild) project.

## License

Portions under Apache, Erlang, and other licenses.

The overall package is released under the Apache license, 2.0.

Copyright 2011, Couchbase, Inc.
