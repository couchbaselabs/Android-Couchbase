package com.couchone.libcouch;

interface ICouchClient
{
    /* Callback to notify when CouchDB has started */
	void couchStarted(String host, int port);
	
	/* Callback notifies when the database requested
	 * has been created
	 */
	void databaseCreated(String dbName, String user, String pass, String tag);
}