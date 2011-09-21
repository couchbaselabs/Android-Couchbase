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
	 */
    void startCouchbase(ICouchbaseDelegate callback);

}
