package se.msc.android.droidcouch;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class CouchBulkDocuments implements ICanJson {

	protected List<ICouchDocument> Docs;

	public CouchBulkDocuments(List<ICouchDocument> docs)
	{
	    Docs = docs;
	}

	public CouchBulkDocuments(ICouchDocument[] docs)
	{
	    Docs = new ArrayList<ICouchDocument>();
	    for (ICouchDocument doc: docs ) {
	    	Docs.add(doc);
	    }
	}

	public List<ICouchDocument> Docs() {
		return Docs;
	}
	public void Docs(List<ICouchDocument> docs) {
		Docs = docs;
	}
	
	
	public int Count()
	{
	    return Docs.size();
	}
	
	public void WriteJson(JSONStringer writer) throws JSONException
	{
	    writer.key("docs");
	    writer.array();
	    for (ICouchDocument doc : Docs)
	    {
	        writer.object();
	        doc.WriteJson(writer);
	        writer.endObject();
	    }
	    writer.endArray();
	}
	
	public void ReadJson(JSONObject obj)
	{
        throw new UnsupportedOperationException();
	}

}
