package se.msc.android.droidcouch;

import java.util.Map;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class CouchDocument implements ICouchDocument {

	public CouchDocument(String id, String rev)
    {
        Id = id;
        Rev = rev;
    }

    public CouchDocument(String id)
    {
        Id = id;
    }

    public CouchDocument()
    {
    }

    public CouchDocument(Map<String, ?> doc)
    {
//      : this(doc["_id"].Value<String>(), doc["_rev"].Value<String>())
    	Id = (String)doc.get("_id");
    	Rev = (String)doc.get("_rev");
    }

    public String Id;
    public String Rev;

    public void WriteJson(JSONStringer writer) throws JSONException 
    {
        try {
			WriteIdAndRev(this, writer);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void ReadJson(JSONObject obj) throws Exception
    {
        try {
			ReadIdAndRev(this, obj);
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void WriteJsonObject(JSONStringer writer) throws JSONException
    {
        writer.object();
        WriteJson(writer);
        writer.endObject();
    }

    public static String WriteJson(ICanJson doc)
    {
        JSONStringer jsonWriter = new JSONStringer();
        
        try {
			jsonWriter.object();
			doc.WriteJson(jsonWriter);
			jsonWriter.endObject();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
        
        return jsonWriter.toString();
    }

    public static void WriteIdAndRev(ICouchDocument doc, JSONStringer writer) throws JSONException
    {
        if (doc.Id() != null)
        {
            writer.key("_id").value(doc.Id());
        }
        if (doc.Rev() != null)
        {
            writer.key("_rev").value(doc.Rev());
        }
    }

    public static void ReadIdAndRev(ICouchDocument doc, JSONObject obj) throws JSONException
    {
    	doc.Id(obj.getString("_id"));
    	doc.Rev(obj.getString("_rev"));
    }

	@Override
	public String Id() {
		return Id;
	}

	@Override
	public void Id(String newValue) {
		Id = newValue;
	}

	@Override
	public String Rev() {
		return Rev;
	}

	@Override
	public void Rev(String newValue) {
		Rev = newValue;
	}

}
