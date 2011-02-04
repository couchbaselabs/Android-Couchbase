package se.msc.android.droidcouch;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class CouchDesignDocument extends CouchDocument {
    public List<CouchViewDefinition> Definitions = new ArrayList<CouchViewDefinition>();
    
    // This List is only used if you also have Couchdb-Lucene installed
//    public List<CouchLuceneViewDefinition> LuceneDefinitions = new List<CouchLuceneViewDefinition>();

    public String Language = "javascript";
    public CouchDatabase Owner;

    public CouchDesignDocument(String documentId, CouchDatabase owner)
    {
    	super("_design/" + documentId);
        Owner = owner;
    }

    public CouchDesignDocument()
    {
        
    }

    /// <summary>
    /// Add view without a reduce function.
    /// </summary>
    /// <param name="name">Name of view</param>
    /// <param name="map">Map function</param>
    /// <returns></returns>
    public CouchViewDefinition AddView(String name, String map)
    {
        return AddView(name, map, null);
    }

    /// <summary>
    /// Add view with a reduce function.
    /// </summary>
    /// <param name="name">Name of view</param>
    /// <param name="map">Map function</param>
    /// <param name="reduce">Reduce function</param>
    /// <returns></returns>
    public CouchViewDefinition AddView(String name, String map, String reduce)
    {
    	CouchViewDefinition def = new CouchViewDefinition(name, map, reduce, this);
        Definitions.add(def);
        return def;
    }

    public void RemoveViewNamed(String viewName)
    {
        RemoveView(FindView(viewName));
    }

    private CouchViewDefinition FindView(String name)
    {
    	for (Iterator<CouchViewDefinition> i = Definitions.iterator(); i.hasNext();)
    	{
    		CouchViewDefinition def = i.next();
    		if(def.Name == name) return def;
    	}
    	return null;
    }

    public void RemoveView(CouchViewDefinition view)
    {
        view.Doc = null;
        Definitions.remove(view);
    }

    /// <summary>
    /// Add Lucene fulltext view.
    /// </summary>
    /// <param name="name">Name of view</param>
    /// <param name="index">Index function</param>
    /// <returns></returns>
/*    public CouchLuceneViewDefinition AddLuceneView(String name, String index)
    {
        var def = new CouchLuceneViewDefinition(name, index, this);
        LuceneDefinitions.Add(def);
        return def;
    }
*/
    /// <summary>
    /// Add a Lucene view with a predefined index function that will index EVERYTHING.
    /// </summary>
    /// <returns></returns>
    /*public CouchLuceneViewDefinition AddLuceneViewIndexEverything(String name)
    {
        return AddLuceneView(name,
                             @"function(doc) {
                                var ret = new Document();

                                function idx(obj) {
                                for (var key in obj) {
                                    switch (typeof obj[key]) {
                                    case 'object':
                                    idx(obj[key]);
                                    break;
                                    case 'function':
                                    break;
                                    default:
                                    ret.add(obj[key]);
                                    break;
                                    }
                                }
                                };

                                idx(doc);

                                if (doc._attachments) {
                                for (var i in doc._attachments) {
                                    ret.attachment(""attachment"", i);
                                }
                                }}");
    }*/

    // All these three methods duplicated for Lucene views, perhaps we should hold them all in one List?
/*    public void RemoveLuceneViewNamed(String viewName)
    {
        RemoveLuceneView(FindLuceneView(viewName));
    }

    private CouchLuceneViewDefinition FindLuceneView(String name)
    {
        return LuceneDefinitions.Where(x => x.Name == name).First();
    }

    public void RemoveLuceneView(CouchLuceneViewDefinition view)
    {
        view.Doc = null;
        LuceneDefinitions.Remove(view);
    }
*/
    /// <summary>
    /// If this design document is missing in the database,
    /// or if it is different - then we save it overwriting the one in the db.
    /// </summary>
    public void Synch() throws CouchException 
    {
        if (!Owner.HasDocument(this)) {
            Owner.SaveDocument(this);
        } else
        {
            CouchDesignDocument docInDb = Owner.GetDocument(CouchDesignDocument.class, Id);
            if (!docInDb.Equals(this)) {
                // This way we forcefully save our version over the one in the db.
                Rev = docInDb.Rev;
                Owner.WriteDocument(this);
            }
        }
    }

    public void WriteJson(JSONStringer writer) throws JSONException
    {
        WriteIdAndRev(this, writer);
        
        writer.key("language").value(Language);

        writer.key("views").object();
      	for (Iterator<CouchViewDefinition> i = Definitions.iterator(); i.hasNext();)
    		i.next().WriteJson(writer);
      	writer.endObject();
      	
        for ( CouchViewDefinition definition : Definitions)
        {
            definition.WriteJson(writer);
        }
        writer.endObject();
        
        // If we have Lucene definitions we write them too
    /*    if (LuceneDefinitions.Count > 0)
        {
            writer.key("fulltext");
            writer.object();
            foreach (var definition in LuceneDefinitions)
            {
                definition.WriteJson(writer);
            }
            writer.WriteEndObject();
        }
     */        
    }

    public void ReadJson(JSONObject obj) throws Exception
    {
        ReadIdAndRev(this, obj);
        if (obj.has("language"))
            Language = obj.getString("language");
        Definitions = new ArrayList<CouchViewDefinition>();
        JSONObject views = obj.getJSONObject("views");

        for (Iterator property = views.keys(); property.hasNext(); )
        {
        	String key = (String)property.next();
            CouchViewDefinition v = new CouchViewDefinition(key, this);
            v.ReadJson(views.getJSONObject(key));
            Definitions.add(v);
        }

/*        JSONObject fulltext = obj.getJSONObject("fulltext");
       // If we have Lucene definitions we read them too
        if (fulltext != null)
        {
            foreach (var property in fulltext.Properties())
            {
                var v = new CouchLuceneViewDefinition(property.Name, this);
                v.ReadJson((JObject) views[property.Name]);
                LuceneDefinitions.Add(v);
            }
        }
*/        
    }

    public boolean Equals(CouchDesignDocument other)
    {
        return Id.equals(other.Id) && Language.equals(other.Language) && Definitions.equals(other.Definitions);
    }

}
