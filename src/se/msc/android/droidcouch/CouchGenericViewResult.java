package se.msc.android.droidcouch;

import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CouchGenericViewResult extends CouchViewResult {

	
	/// <summary>
    /// Return all found values as documents of given type
    /// </summary>
    /// <typeparam name="T">Type of value.</typeparam>
    /// <returns>All found values.</returns>
    public <T extends ICanJson> List<T> ValueDocuments(Class<T> c) throws JSONException
    {
        return RetrieveDocuments(c, "value");
    }

    /// <summary>
    /// Return first value found as document of given type.
    /// </summary>
    /// <typeparam name="T">Type of value</typeparam>
    /// <returns>First value found or null if not found.</returns>
    public <T extends ICanJson> T ValueDocument(Class<T> c) throws JSONException
    {
        return RetrieveDocument(c, "value");
    }

    /// <summary>
    /// Return all found docs as documents of given type
    /// </summary>
    /// <typeparam name="T">Type of documents.</typeparam>
    /// <returns>List of documents found.</returns>
    public <T extends ICouchDocument> List<T> Documents(Class<T> c) 
    {
        return RetrieveDocuments(c,"doc");
    }

    /// <summary>
    /// Return all found docs as CouchJsonDocuments.
    /// </summary>
    /// <returns>List of documents found.</returns>
    public List<CouchJsonDocument> Documents() 
    {
        return RetrieveDocuments(CouchJsonDocument.class,"doc");
    }

    /// <summary>
    /// Return first document found as document of given type
    /// </summary>
    /// <typeparam name="T">Type of document</typeparam>
    /// <returns>First document found or null if not found.</returns>
    public <T extends ICouchDocument> T Document(Class<T> c) throws JSONException
    {
        return RetrieveDocument(c,"doc");
    }

    protected <T extends ICanJson> List<T> RetrieveDocuments(Class<T> c, String docOrValue)
    {
		List<T> list = new ArrayList<T>();
		JSONArray rows = this.Rows();
        
		try {
			for (int i = 0; i< rows.length(); i++)
			{
				JSONObject row = rows.getJSONObject(i);
				T inst = c.newInstance();
				inst.ReadJson(row.getJSONObject(docOrValue));
				list.add(inst);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
        return list;
    }

    protected <T extends ICanJson> T RetrieveDocument(Class<T> c, String docOrValue) throws JSONException
    {
    	JSONArray rows = Rows();
		try {
			if (rows.length()>0) {
				T inst;
				inst = c.newInstance();
				inst.ReadJson(rows.getJSONObject(0));
				return inst;
			}
		} catch (Exception e) {
			// do nothing
		}
        return null;
    }

    public <T extends ICanJson> List<T> RowDocuments(Class<T> c) 
    {
    	List<T> list = new ArrayList<T>();
    	JSONArray rows = Rows();
		for (int i = 0; i< rows.length(); i++)
		{
			JSONObject row = rows.optJSONObject(i);
			try {
				T inst = c.newInstance();
				inst.ReadJson(row);
				list.add(inst);
			} catch (Exception e) {
			}
		}
   	
        return list; //RetrieveDocuments<CouchQueryDocument>();
    }

   public List<CouchQueryDocument> RowDocuments() 
   {
	   return RowDocuments(CouchQueryDocument.class);
   }
}
