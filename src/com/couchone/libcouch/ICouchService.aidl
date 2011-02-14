package com.couchone.libcouch;

import com.couchone.libcouch.ICouchClient;

interface ICouchService
{
    /* Starts couchDB, calls "couchStarted" callback when 
     * complete 
     */
    void initCouchDB(ICouchClient callback);
    
    /* The database may not be named as hinted here, this is to
     * prevent conflicts, cmdDb is not currently used
     */
    void initDatabase(ICouchClient callback, String name, String pass, boolean cmdDb);

    /*
     * 
     */
    void quitCouchDB();
}
