package com.couchone.libcouch;

interface ICouchClient
{
    /* Callback to notify when CouchDB has started */
	void couchStarted(String host, int port);
	
	/* Callback notifies when the database requested
	 * has been created, each database has a related
	 * Control Database, which is used to manage replication
	 * and sharing etc, both use the same credentials
	 */
	void databaseCreated(String dbName, String user, String pass, String tag);
}