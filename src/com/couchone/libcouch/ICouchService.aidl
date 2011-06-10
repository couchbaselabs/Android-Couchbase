package com.couchone.libcouch;

import com.couchone.libcouch.ICouchClient;

interface ICouchService
{
    /* Starts couchDB, calls "couchStarted" callback when 
     * complete 
     */
    void initCouchDB(ICouchClient callback, String url, String pkg);
    
    /*
     * 
     */
    void quitCouchDB();
}
