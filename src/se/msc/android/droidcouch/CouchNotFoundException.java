package se.msc.android.droidcouch;

public class CouchNotFoundException extends CouchException {
	/**
	 * 
	 */
	private static final long serialVersionUID = 5472432541870905872L;

	public CouchNotFoundException(String msg, Exception e)
    {
		super(msg,e);
    }

}
