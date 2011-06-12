package com.couchbase.libcouch;

interface ICouchClient
{
    /* Callback to notify when CouchDB has started */
	void couchStarted(String host, int port);

    /* Callback for notifications on how the CouchDB install is progressing */
	void progress(int status, int completed, int total);
}