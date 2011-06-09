package com.couchone.libcouch;

interface ICouchClient
{
    /* Callback to notify when CouchDB has started */
	void couchStarted(String host, int port);
}