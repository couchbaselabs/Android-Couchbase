package se.msc.android.droidcouch;

import java.util.Iterator;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class CouchJsonDocument implements ICouchDocument {
    protected String Id;
    protected String Rev;
    public JSONObject Obj;
    
    private JSONObject newJson(String json) {
    	try {
    		return new JSONObject(json);
    	} catch (JSONException e) { 
   		}
		return new JSONObject(); 
    }
    
	public CouchJsonDocument(String json, String id, String rev)
    {
		Obj = newJson(json);
		Id = id;
		Rev = rev;
    }

    public CouchJsonDocument(String json, String id)
    {
   		Obj = newJson(json);
   		Id = id;
    }

    public CouchJsonDocument(String json) 
    {
   		Obj = newJson(json);
    }

    public CouchJsonDocument(JSONObject doc)
    {
        Obj = doc;
    }

    public CouchJsonDocument()
    {
        Obj = new JSONObject();
    }

    public String toString()
    {
        return Obj.toString();
    }


 
    public void WriteJson(JSONStringer writer) 
    {
    	Iterator i = Obj.keys();
    	while (i.hasNext()) {
    		String key = (String) i.next();
    		try {
				writer.key(key).value(Obj.get(key));
			} catch (JSONException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
    	}
    }

    // Presume that Obj has _id and _rev
    public void ReadJson(JSONObject obj)
    {
        Obj = obj;
    }

    public String Rev()
    {
    	return Obj.optString("_rev");
    }
    public void Rev(String newValue)
    {
    	try {
			Obj.put("_rev", newValue);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public String Id()
    {
    	return Obj.optString("_id");
    }
    public void Id(String newValue)
    {
    	try {
			Obj.put("_id", newValue);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
 
}
