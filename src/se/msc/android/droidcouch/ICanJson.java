package se.msc.android.droidcouch;

import org.json.JSONException;
import org.json.JSONObject;
import org.json.JSONStringer;

public interface ICanJson {

	
    /// Basic capability to write and read myself using JSON.
    /// Writing is done using JSONStringer in a fast streaming fashion.
    /// Reading is done using JSONObject "DOM style".

	
	void WriteJson(JSONStringer writer) throws JSONException;
    void ReadJson(JSONObject obj) throws Exception;

}
