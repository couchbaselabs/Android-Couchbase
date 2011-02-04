package se.msc.android.droidcouch;

import java.util.AbstractCollection;
import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public class CouchBulkKeys implements ICanJson {
	
    public <T> CouchBulkKeys(AbstractCollection<T> keys)
    {
        Keys = keys.toArray();
    }

    public CouchBulkKeys()
    {
    }

    public CouchBulkKeys(Object[] keys)
    {
        Keys = keys;
    }

    public Object[] Keys;


    public void WriteJson(JSONStringer writer)
    {
        try {
			writer.key("keys").array();
	        for (Object id: Keys)
	            writer.value(id);
	        writer.endArray();
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }

    public void ReadJson(JSONObject obj) 
    {
    	// not implemented
        throw new UnsupportedOperationException();
    }

    public int Count()
    {
        return Keys.length;
    }
}
