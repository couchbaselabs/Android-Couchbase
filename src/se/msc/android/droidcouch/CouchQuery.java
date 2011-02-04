package se.msc.android.droidcouch;

import java.io.IOException;
import java.util.AbstractCollection;
import java.util.Dictionary;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONObject;

public class CouchQuery {
	protected CouchViewDefinition View;
	
    public CouchViewDefinition View() { return View; };

    // Special options
    public boolean checkETagUsingHead;
    public Map<String, String> Options = new HashMap<String, String>();
    public String postData;
    public CouchViewResult Result;

    public CouchQuery(CouchViewDefinition view)
    {
        View = view;
    }

    public void ClearOptions()
    {
        Options = new HashMap<String, String>();
    }

    /// <summary>
    /// Setting POST data which will automatically trigger the query to be a POST request.
    /// </summary>
    public CouchQuery Data(String data)
    {
        postData = data;
        return this;
    }


    /// <summary>
    /// This is a bulk key request, not to be confused with requests using complex keys, see Key().
    /// </summary>
    public CouchQuery Keys(Object[] keys)
    {
    	CouchBulkKeys bulk = new CouchBulkKeys(keys);
        Data(CouchDocument.WriteJson(bulk));
        return this;
    }

    /// <summary>
    /// This is a bulk key request, not to be confused with requests using complex keys, see Key().
    /// </summary>
    public <T> CouchQuery Keys(AbstractCollection<T> keys)
    {
    	CouchBulkKeys bulk = new CouchBulkKeys(keys);
        Data(CouchDocument.WriteJson(bulk));
        return this;
    }

    public CouchQuery setOption(String key, String value)
    {
    	Options.put(key, value);
    	return this;
    }
    public CouchQuery setOption(String key, boolean value)
    {
    	if(value)
    		return setOption(key,"true");
    	return setOption(key,"false");
    }
    
    public CouchQuery setOption(String key, Object value)
    {
    	if (value == null)
    		return setOption(key, "null");
    	return setOption(key,value.toString());
    }

    public CouchQuery setOption(String key, int value)
    {
  		return setOption(key, String.valueOf(value));
    }

    /// <summary>
    /// Any valid JSON value is a valid key. This means:
    ///  null, true, false, a string, a number, a Dictionary (JSON object) or an array (JSON array)
    /// </summary>
    public CouchQuery Key(Object value)
    {
        return setOption("key", value.toString());
    }

    /// <summary>
    /// Any valid JSON value is a valid key. This means:
    ///  null, true, false, a string, a number, a Dictionary (JSON object) or an array (JSON array)
    /// </summary>
/*    public CouchQuery Key(Object[] value)
    {
    	return null;
//        return setOption("key", value);
    }
*/
    /// <summary>
    /// Any valid JSON value is a valid key. This means:
    ///  null, true, false, a string, a number, a Dictionary (JSON object) or an array (JSON array)
    /// </summary>
    public CouchQuery StartKey(Object value)
    {
        return setOption("startkey", value);
    }

    /// <summary>
    /// Any valid JSON value is a valid key. This means:
    ///  null, true, false, a string, a number, a Dictionary (JSON object) or an array (JSON array)
    /// </summary>
/*    public CouchQuery StartKey(Object[] value)
    {
        return setOption("startkey", value);
    }
*/
    public CouchQuery StartKeyDocumentId(String value)
    {
        return setOption("startkey_docid", value);
    }

    /// <summary>
    /// Any valid JSON value is a valid key. This means:
    ///  null, true, false, a string, a number, a Dictionary (JSON object) or an array (JSON array)
    /// </summary>
    public CouchQuery EndKey(Object value)
    {
        return setOption("endkey", value);
    }

    /// <summary>
    /// Any valid JSON value is a valid key. This means:
    ///  null, true, false, a string, a number, a Dictionary (JSON object) or an array (JSON array)
    /// </summary>
/*    public CouchQuery EndKey(Object[] value)
    {
        return setOption("endkey", value);
    }
*/
    public CouchQuery EndKeyDocumentId(String value)
    {
        return setOption("endkey_docid", value);
    }

    public CouchQuery Limit(int value)
    {
        return setOption("limit", value);
    }

    public CouchQuery Stale()
    {
        return setOption("stale", "ok");
    }

    public CouchQuery Descending()
    {
        return setOption("descending", true);
    }

    public CouchQuery Skip(int value)
    {
        return setOption("skip", value);
    }

    public CouchQuery Group()
    {
        return setOption("group", true);
    }

    public CouchQuery GroupLevel(int value)
    {
        return setOption("group_level", value);
    }

    public CouchQuery Reduce()
    {
        return setOption("reduce", true);
    }

    public CouchQuery IncludeDocuments()
    {
        return setOption("include_docs", true);
    }

    /// <summary>
    /// Tell this query to do a HEAD request first to see
    /// if ETag has changed and only then do the full request.
    /// This is only interesting if you are reusing this query object.
    /// </summary>
    public CouchQuery CheckETagUsingHead()
    {
        checkETagUsingHead = true;
        return this;
    }

    public CouchGenericViewResult GetResult() throws CouchException
    {
    	try {
			return GetResult(CouchGenericViewResult.class);
		} catch (Exception e) {
			throw CouchException.Create("Query failed", e);
		}   
    }

    public boolean IsCachedAndValid() 
    {
        // If we do not have a result it is not cached
        if (Result == null)
        {
            return false;
        }
        CouchRequest req = View.Request().QueryOptions(Options);
        req.Etag(Result.etag);
        try {
        	return req.Head().Send().IsETagValid();
        } catch (Exception e) {
        	return false;
        }
    }


    public String String()
    {
        return Request().String();
    }


    public CouchRequest Request()
    {
    	CouchRequest req = View.Request().QueryOptions(Options);
        if (postData != null)
        {
            req.Data(postData).Post();
        }
        return req;
    }

    public <T extends CouchViewResult> T GetResult(Class<T> c) throws IllegalAccessException, InstantiationException, ClientProtocolException, IOException
    {
        CouchRequest req = Request();

        if (Result == null)
        {
            Result = c.newInstance();
        }
        else
        {
            // Tell the request what we already have
            req.Etag(Result.etag);
            if (checkETagUsingHead)
            {
                // Make a HEAD request to avoid transfer of data
                if (req.Head().Send().IsETagValid())
                {
                    return (T)Result;
                }
                // Set back to GET before proceeding below
                req.Get();
            }
        }

        JSONObject json = req.Parse();
        if (json != null) // ETag did not match, view has changed
        {
            Result.Result(json);
            Result.etag = req.Etag();
        }
        return (T) Result;
    }
}
