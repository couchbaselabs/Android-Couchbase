package com.couchbase.android;

/**
 *  The delegate interface a client should implement to receive
 *  notifications of important events from the Couchbase service
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
     * Callback for notification that Couchbase has exited
     *
     * @param error an error message describing the reason Couchbase exited
     */
    void exit(String error);
}