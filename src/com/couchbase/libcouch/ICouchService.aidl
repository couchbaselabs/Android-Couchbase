package com.couchbase.libcouch;

import com.couchbase.libcouch.ICouchClient;

interface ICouchService
{
    /* Starts couchDB, calls "couchStarted" callback when 
     * complete 
     */
    void initCouchDB(ICouchClient callback, String pkg);
    
    /*
     * 
     */
    void quitCouchDB();
}
