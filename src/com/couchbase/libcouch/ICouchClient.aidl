package com.couchbase.libcouch;

interface ICouchClient
{
    /* Callback to notify when CouchDB has started */
	void couchStarted(String host, int port);

    /* Callback for notifications on how the CouchDB install is progressing */
    void installing(int completed, int total);

    void exit(String error);
}