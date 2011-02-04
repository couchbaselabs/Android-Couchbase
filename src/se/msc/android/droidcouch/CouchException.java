package se.msc.android.droidcouch;

import java.io.IOException;

import org.apache.http.HttpStatus;
import org.apache.http.client.HttpResponseException;

public class CouchException extends Exception {
    /**
	 * 
	 */
	private static final long serialVersionUID = 2320240482672506994L;
	public HttpStatus StatusCode;

    public CouchException()
    {
    }

    public CouchException(String message)
    {
    	super(message);
    }

    public CouchException(String message, Exception innerException) 
    {
    	super(message, innerException);
    }

/*    protected CouchException(SerializationInfo info, StreamingContext context) : base(info, context)
    {
    }
*/
    public static CouchException Create(String message)
    {
        return new CouchException(message);
    }

    public static CouchException Create(String message, Exception e)
    {
        String msg = String.format(message + ": %s", e.getLocalizedMessage());
        if (e instanceof HttpResponseException)
        {
        	HttpResponseException eResp = (HttpResponseException)e;
        	int code = eResp.getStatusCode();
        
        	if (code == HttpStatus.SC_CONFLICT)
        	{
        		return new CouchConflictException(msg,e);
        	}
        	if (code == HttpStatus.SC_NOT_FOUND)
        	{
        		return new CouchNotFoundException(msg,e);
        	}
        	        	
        }

        // Fall back on generic CouchException
        return new CouchException(msg, e);
    }
}
