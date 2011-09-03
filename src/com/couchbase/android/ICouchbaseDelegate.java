package com.couchbase.android;

/**
 * 
 * 
 *
 */
public interface ICouchbaseDelegate
{

	/**
	 * Callback to notify when Couchbase has started
	 * 
	 * @param host the host Couchbase is listening on
	 * @param port the port Couchbase is listening on
	 */
	void couchbaseStarted(String host, int port);

	/**
	 * Callback for notifications on how the Couchbase install is progressing
	 * 
	 * @param completed the number of files installed
	 * @param total the total number of files to install
	 */
    void installing(int completed, int total);

    /**
     * Callback for notification that Couchbase has exited
     * 
     * @param error an error message describing the reason Couchbase exited
     */
    void exit(String error);
}