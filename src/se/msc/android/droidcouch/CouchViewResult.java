package se.msc.android.droidcouch;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CouchViewResult {
    public String etag;
    public JSONObject result;

    public void Result(JSONObject obj)
    {
        result = obj;
    }

    public int TotalCount() throws JSONException
    {
        return (int)result.getLong("total_rows");
    }

    public int Offset() throws JSONException
    {
        return (int)result.getLong("offset");
    }

    public JSONArray Rows()
    {
    	try {
    		return result.getJSONArray("rows");
    	} catch (JSONException e) {
    		
    	}
    	
    	return new JSONArray();
        //return result["rows"].Children();
    }

    public int Count() throws JSONException
    {
    	return this.Rows().length();
        //return result["rows"].Value<JArray>().Count;
    }
}
