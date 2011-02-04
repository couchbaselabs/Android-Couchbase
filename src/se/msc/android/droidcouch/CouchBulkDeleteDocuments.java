package se.msc.android.droidcouch;

import java.util.List;

import org.json.JSONException;
import org.json.JSONStringer;


public class CouchBulkDeleteDocuments extends CouchBulkDocuments {

   
    public CouchBulkDeleteDocuments(ICouchDocument[] docs) {
		super(docs);
	}

	public CouchBulkDeleteDocuments(List<ICouchDocument> documents) {
		super(documents);
	}

	public void WriteJson(JSONStringer writer) throws JSONException
    {
	    writer.key("docs");
	    writer.array();
	    for (ICouchDocument doc : Docs)
	    {
	        writer.object();
            CouchDocument.WriteIdAndRev(doc, writer);
            writer.key("_deleted").value(true);
	        
	        writer.endObject();
	    }
	    writer.endArray();
    	
    }

}
