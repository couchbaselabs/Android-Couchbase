package com.couchbase.android;

import com.couchbase.android.ICouchbaseDelegate;

interface ICouchbaseService
{
    /* Starts couchbase, calls "couchbaseStarted" callback when 
     * complete 
     */
    void startCouchbase(ICouchbaseDelegate callback, String pkg);
    
    void stopCouchbase();
}
