package com.couchbase.libcouch;

import com.couchbase.libcouch.ICouchClient;

interface ICouchService
{
    /* Starts couchDB, calls "couchStarted" callback when 
     * complete 
     */
    void startCouchbase(ICouchClient callback, String pkg);
    
    void stopCouchbase();
}
