package com.couchbase.android;

import android.content.Intent;

public class Intents {

	/**
	 * Do not instantiate this class
	 */
	private Intents() {}

	/**
	 * An intent used to notify that Couchbase has started
	 */
	public static final class CouchbaseStarted {

		/**
		 * Do not instantiate this class
		 */
		private CouchbaseStarted() {}

		/**
		 * The action name
		 */
		public final static String ACTION = "com.couchbase.android.COUCHBASE.STARTED";

		/**
		 * Name used in Intent Extra for storing host
		 */
		public final static String HOST = "host";

		/**
		 * Name used in Intent Extra for storing port
		 */
		public final static String PORT = "port";

		/**
		 * Name used in Intext Extra for storing process id
		 */
		public final static String PID = "pid";

		/**
		 * Utility to get the host Couchbase is listening on
		 * @param intent the intent to parse
		 * @return the hostname
		 */
		public static String getHost(Intent intent) {
			return intent.getStringExtra(HOST);
		}

		/**
		 * Utility to get the port Couchbase is listening on
		 * @param intent the intent to parse
		 * @return the port
		 */
		public static int getPort(Intent intent) {
			return intent.getIntExtra(PORT, -1);
		}

		/**
		 * Utility to get the pid of the process for this Couchbase
		 * @param intent the intent to parse
		 * @return the pid
		 */
		public static int getPid(Intent intent) {
		    return intent.getIntExtra(PID, -1);
		}

	}

	/**
	 * An intent used to notify that Couchbase has encountered an error
	 */
	public static final class CouchbaseError {

		/**
		 * Do not instantiate
		 */
		private CouchbaseError() {}

		/**
		 * The action name
		 */
		public final static String ACTION = "com.couchbase.android.COUCHBASE.ERROR";

		/**
		 * Name used in Intent Extra for storing message
		 */
		public final static String MESSAGE = "message";

        /**
         * Name used in Intext Extra for storing process id
         */
        public final static String PID = "pid";

		/**
		 * Utility to get the message from Couchbase
		 * @param intent the intent to parse
		 * @return the message
		 */
		public static String getMessage(Intent intent) {
			return intent.getStringExtra(MESSAGE);
		}

        /**
         * Utility to get the pid of the process for this Couchbase
         * @param intent the intent to parse
         * @return the pid
         */
        public static int getPid(Intent intent) {
            return intent.getIntExtra(PID, -1);
        }

	}

}
