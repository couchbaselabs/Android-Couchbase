package com.couchbase.android;

interface ICouchbaseDelegate
{
    /* Callback to notify when Couchbase has started */
	void couchbaseStarted(String host, int port);

    /* Callback for notifications on how the Couchbase install is progressing */
    void installing(int completed, int total);

    void exit(String error);
}