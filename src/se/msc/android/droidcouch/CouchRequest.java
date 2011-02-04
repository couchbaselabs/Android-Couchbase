package se.msc.android.droidcouch;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.http.Header;
import org.apache.http.HttpEntity;
import org.apache.http.HttpRequest;
import org.apache.http.HttpResponse;
import org.apache.http.client.ClientProtocolException;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpEntityEnclosingRequestBase;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpHead;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpRequestBase;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONException;
import org.json.JSONObject;

import android.util.Log;

public class CouchRequest {

    private CouchDatabase db;
    private CouchServer server;
    private String etag, etagToCheck;
    public Map<String, String> headers = new HashMap<String, String>();

    // Query options
   public HttpRequestBase method = new HttpGet(); // PUT, DELETE, POST, HEAD

    public String mimeType;
    public String path;
    public String postData;
    public String query;

   public JSONObject result;

    public CouchRequest(CouchServer server)
    {
        this.server = server;
    }

    public CouchRequest(CouchDatabase db)
    {
        server = db.Server;
        this.db = db;
    }

    public CouchRequest Etag(String value)
    {
        etagToCheck = value;
        headers.put("If-Modified", value);
        return this;
    }

    public CouchRequest Path(String name)
    {
        path = name;
        return this;
    }

    public CouchRequest Query(String name)
    {
        query = name;
        return this;
    }

    public CouchRequest QueryOptions(Map<String, String> options)
    {
        if (options == null || options.isEmpty())
        {
            return this;
        }

        StringBuilder sb = new StringBuilder();
        sb.append("?");

        Iterator<Entry<String, String>> i = options.entrySet().iterator();
        while (i.hasNext())
        {
        	Entry<String, String> e = i.next();
            if (sb.length() > 1)
            {
                sb.append("&");
            }
            try {
            	sb.append(URLEncoder.encode(e.getKey(), "UTF-8"));
            	sb.append("=");
            	sb.append(URLEncoder.encode(e.getValue(), "UTF-8"));
            } catch (Exception ex) 
            {
				// TODO Auto-generated catch block
            }
        }

        return Query(sb.toString());
    }

    /// <summary>
    /// Turn the request into a HEAD request, HEAD requests are problematic
    /// under Mono 2.4, but has been fixed in later releases.
    /// </summary> 
    public CouchRequest Head()
    {
        method = new HttpHead();
        return this;
    }

    public CouchRequest PostJson()
    {
        MimeTypeJson();
        return Post();
    }

    public CouchRequest Post()
    {
        method = new HttpPost();
        return this;
    }

    public CouchRequest Get()
    {
        method = new HttpGet();
        return this;
    }

    public CouchRequest Put()
    {
        method = new HttpPut();
        return this;
    }

    public CouchRequest Delete()
    {
        method = new HttpDelete();
        return this;
    }

    public CouchRequest Data(String data)
    {
        postData = data;
        return this;
    }

    public CouchRequest MimeType(String type)
    {
        mimeType = type;
        return this;
    }

    public CouchRequest MimeTypeJson()
    {
        MimeType("application/json");
        return this;
    }

    public JSONObject Result()
    {
        return (JSONObject) result;
    }

/*    public <T> T Result()
    {
        return (T) result;
    }
*/
    public String Etag()
    {
        return etag;
    }

    public CouchRequest Check(String message) throws CouchException
    {
        try
        {
            if (result == null)
            {
                Parse();
            }
            if (!result.has("ok"))
            {
                throw CouchException.Create(String.format(message + ": %s", result));
            }
            return this;
        }
        catch (Exception e)
        {
        	if (! (e instanceof  CouchException) )
        		throw CouchException.Create(message, e);
        	else
        		throw (CouchException)e;
        }
    }

    private HttpRequest GetRequest() throws URISyntaxException, UnsupportedEncodingException
    {
    	
        URI requestUri = new URI(
        		"http", "",
        		server.Host, 
        		server.Port, 
        		((db != null) ? db.Name() + "/" : "") + path ,
        		query,"");
        method.setURI(requestUri);
        
        if (mimeType != null)
        {
        	method.addHeader("Content-type", mimeType);
        }        

        if (postData != null)
        {
        	StringEntity e = new StringEntity(postData, "UTF-8");
        	((HttpEntityEnclosingRequestBase)method).setEntity(e);
        }

       Log.i(DroidCouch.TAG, String.format("Request: %s Method: %s",requestUri.toString(), method.getMethod()));
        return method;
    }

    public JSONObject Parse()
    {
    	String result = String();
    	if (result !=null)
			try {
				return new JSONObject(result);
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				/*e.printStackTrace();*/
			}
    	return null;
    }


    private void PickETag(HttpResponse response)
    {
    	Header hdr = response.getFirstHeader("ETag");
        if (hdr != null)
        {
        	etag = hdr.getValue();
            etag = etag.endsWith("\"") ? etag.substring(1, etag.length() - 2) : etag;
        }
    }

    /// <summary>
    /// Return the request as a plain String instead of trying to parse it.
    /// </summary>
    public String String()
    {
    	HttpResponse response = null;
    	
		try {
			response = GetResponse();
	    	PickETag(response);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	
    	if (etagToCheck != null)
	    {
    		if (IsETagValid())
	        {
	            return null;
	        }
	    }
        HttpEntity entity = response.getEntity();
        // If the response does not enclose an entity, there is no need
        // to worry about connection release

        String result = null;
        if (entity != null) {
            // A Simple JSON Response Read
            InputStream instream;
			try {
				instream = entity.getContent();
	            result = DroidCouch.convertStreamToString(instream);
	            // Log.i(TAG, result);

	            // Closing the input stream will trigger connection release
	            instream.close();
			} catch (Exception e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				return null;
			}
        }
        return result;
    }

    private HttpResponse GetResponse() throws ClientProtocolException, IOException
    {
        return (new DefaultHttpClient()).execute(method);
    }

    public CouchRequest Send() throws ClientProtocolException, IOException
    {
		HttpResponse response = GetResponse();
    	PickETag(response);

    	return this;
    }

    public Boolean IsETagValid()
    {
        return etagToCheck == etag;
    }

}
