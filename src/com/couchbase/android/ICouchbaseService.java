package com.couchbase.android;


/**
 *  The public interface to the Couchbase service
 */

interface ICouchbaseService
{

	/**
	 * Starts Couchbase service asynchronously.  Delegate will be notified
     * when the service is ready.
	 *
	 * @param callback the delegate to receive notifications from this service
	 * @param pkg the version string the installer will verify, and install if necessary
	 */
    void startCouchbase(ICouchbaseDelegate callback, String pkg);

    /**
     * Stops the Couchbase service.
     */
    void stopCouchbase();
}
