package se.msc.android.droidcouch;

import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class CouchViewDefinition extends CouchViewDefinitionBase {
    /// <summary>
    /// Basic constructor used in ReadJson() etc.
    /// </summary>
    /// <param name="name">View name used in URI.</param>
    /// <param name="doc">A design doc, can also be created on the fly.</param>
    public CouchViewDefinition(String name, CouchDesignDocument doc) {
    	super(name, doc);
    }

    /// <summary>
    /// Constructor used for permanent views, see CouchDesignDocument.
    /// </summary>
    /// <param name="name">View name.</param>
    /// <param name="map">Map function.</param>
    /// <param name="reduce">Optional reduce function.</param>
    /// <param name="doc">Parent document.</param>
    public CouchViewDefinition(String name, String map, String reduce, CouchDesignDocument doc)
    {
    	super(name, doc);
        Map = map;
        Reduce = reduce;
    }

    public String Map;
    public String Reduce;

    public void Touch() throws CouchException
    {
        Query().Limit(0).GetResult();
    }

    public CouchQuery Query()
    {
        return Doc.Owner.Query(this);
    }
/*
	public CouchLinqQuery<T> LinqQuery<T>() {
		var linqProvider = new CouchQueryProvider(Db(), this);
        return new CouchLinqQuery<T>(linqProvider);
	}
*/	
    public void WriteJson(JSONStringer writer) throws JSONException
    {
        writer.key(Name)
        	.object()
        	.key("map").value(Map);

        if (Reduce != null)
        {
            writer.key("reduce").value(Reduce);
        }
        writer.endObject();
    }

    public void ReadJson(JSONObject obj) throws JSONException
    {
        Map = obj.getString("map");
        Reduce = obj.optString("reduce");
        if (Reduce.length() == 0)
        	Reduce = null;
    }

    /// <summary>
    /// Utility methods to make queries shorter.
    /// </summary>
    public <T extends ICouchDocument> List<T> Key(Class<T> c, String key) throws JSONException, CouchException
    {
        return Query().Key(key).IncludeDocuments().GetResult().Documents(c);
    }

    public <T extends ICouchDocument> List<T> KeyStartEnd(Class<T> c, Object start, Object end) throws JSONException, CouchException 
    {
        return Query().StartKey(start).EndKey(end).IncludeDocuments().GetResult().Documents(c);
    }

    public <T extends ICouchDocument> List<T> KeyStartEnd(Class<T>c, Object[] start, Object[] end) throws JSONException, CouchException
    {
        return Query().StartKey(start).EndKey(end).IncludeDocuments().GetResult().Documents(c);
    }

    public <T extends ICouchDocument>  List<T> All(Class<T> c) throws JSONException, CouchException
    {
        return Query().IncludeDocuments().GetResult().Documents(c);
    }

    public Boolean equals(CouchViewDefinition other)
    {
        return 
            Name != null && 
            Name.equals(other.Name) && 
            Map != null &&
            Map.equals(other.Map) && 
            Reduce != null &&
            Reduce.equals(other.Reduce);
    }
}
