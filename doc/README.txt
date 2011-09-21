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

## Examples

For examples please look at:

* https://github.com/couchbase/Android-EmptyApp
* https://github.com/daleharvey/Android-MobileFuton
* https://github.com/couchbaselabs/AndroidGrocerySync

## Join us

There is a Google Group here for Mobile Couchbase at https://groups.google.com/group/mobile-couchbase. Let's talk about how to optimize the Erlang build, what the best Java APIs are for CouchDB, how to take advantage of replication on mobile devices. It'll be fun.

## Develop

Interested in the Android-Couchbase internals?  Check out the GitHub repository https://github.com/couchbase/Android-Couchbase
