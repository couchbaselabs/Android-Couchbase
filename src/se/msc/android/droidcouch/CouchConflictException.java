package se.msc.android.droidcouch;

public class CouchConflictException extends CouchException {

	/**
	 * 
	 */
	private static final long serialVersionUID = 6755631157869078026L;

	public CouchConflictException(String msg, Exception e)
    {
		super(msg,e);
    }

}
