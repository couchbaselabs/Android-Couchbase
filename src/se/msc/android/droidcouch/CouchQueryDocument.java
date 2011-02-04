package se.msc.android.droidcouch;

import org.json.JSONException;
import org.json.JSONObject;

public class CouchQueryDocument extends CouchDocument {
	/// <summary>
	/// This is used to hold only metadata about a document retrieved from view queries.
	/// </summary>
    public String Key;

    public void ReadJson(JSONObject obj)
    {
        try {
			Id = obj.getString("id");
	        Key = obj.getString("key");
	        Rev = obj.getJSONObject("value").getString("rev");
		} catch (JSONException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    }
}
