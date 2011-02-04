/**
 * 
 */
package se.msc.android.droidcouch;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

/*
	A CouchServer is simply a communication end point holding a hostname and a port number to talk to.
	It has an API to list, lookup, create or delete CouchDB "databases" in the CouchDB server.
	One nice approach is to create a specific subclass that knows about its databases.
	DatabasePrefix can be used to separate all databases created from other CouchDB databases.
 */
public class CouchServer {
    
    public static boolean RunningOnMono = false;
	private static String DefaultHost = "localhost";
    private static int DefaultPort = 5984;
//    private readonly JsonSerializer serializer = new JsonSerializer(); 
    
    public String Host;
    public int Port;

    public String DatabasePrefix = ""; // Used by databases to prefix their names

    public CouchServer(String host, int port)
    {
        Host = host;
        Port = port;

        Debug(String.format("CouchServer({0}:{1})", host, port));
    }

    public CouchServer(String host)
    {
    	this(host, DefaultPort);
    }

    public CouchServer()
    {
    	this(DefaultHost, DefaultPort);
    }

    public String ServerName()
    {
      return Host + ":" + Port;
    }

    public CouchRequest Request()
    {
        return new CouchRequest(this);
    }

    /// <summary>
    /// Override this method with some other debug logging.
    /// </summary>
    public void Debug(String message)
    {
        System.out.println(message);
    }

    public Boolean HasDatabase(String name)
    {
        //return GetDatabaseNames().Contains(name); // This is too slow when we have thousands of dbs!!!
        try
        {
            Request().Path(name).Head().Send();
            return true;
        }
        catch (Exception ex)
        {
            return false;
        }
    }

    /// <summary>
    /// Get a CouchDatabase with given name.
    /// We create the database if it does not exist.
    /// </summary>
    public CouchDatabase GetDatabase(String name) throws CouchException 
    {
        return GetDatabase(CouchDatabase.class, name);
    }

    /// <summary>
    /// Get a new CouchDatabase with given name.
    /// We check if the database exists and delete
    /// it if it does, then we recreate it.
    /// </summary>
    public CouchDatabase GetNewDatabase(String name) throws Exception
    {
        return GetNewDatabase(CouchDatabase.class,name);
    }

    /// <summary>
    /// Get specialized subclass of CouchDatabase with given name.
    /// We check if the database exists and delete it if it does,
    /// then we recreate it.
    /// </summary>
    public <T extends CouchDatabase> T GetNewDatabase(Class<T> c, String name) throws Exception
    {
        T db = c.newInstance();
        db.Name(name);
        db.Server = this;
        if (db.Exists())
        {
            db.Delete();
        }
        db.Create();
        return db;
    }

    /// <summary>
    /// Get specialized subclass of CouchDatabase. That class should
    /// define its own database name. We presume it is already created.
    /// </summary>
    
    public <T extends CouchDatabase> T GetExistingDatabase(Class<T> c) throws IllegalAccessException, InstantiationException 
    {
    	T db = c.newInstance();
    	db.Server = this;
        return db;
    }

    /// <summary>
    /// Get specialized subclass of CouchDatabase with given name.
    /// We presume it is already created.
    /// </summary>
    
    
    public <T extends CouchDatabase> T GetExistingDatabase(Class<T> c, String name) throws IllegalAccessException, InstantiationException
    {
    	T db = c.newInstance();
    	db.Name(name);
    	db.Server = this;
        return db;
    }
    /// <summary>
    /// Get specialized subclass of CouchDatabase. That class should
    /// define its own database name. We ensure that it is created.
    /// </summary>
    public<T extends CouchDatabase> T GetDatabase(Class<T> c) throws Exception
    {
        T db = GetExistingDatabase(c);
        db.Create();
        return db;
    }

    /// <summary>
    /// Get specialized subclass of CouchDatabase with given name.
    /// We ensure that it is created.
    /// </summary>
    public <T extends CouchDatabase> T GetDatabase(Class<T> c, String name) throws CouchException 
    {
    	try {
	        T db = GetExistingDatabase(c, name);
	        db.Create();
	        return db;
    	} catch (Exception e){
    		throw CouchException.Create("GetDatabase", e);
    	}
    }


    /// <summary>
    /// Typically only used from CouchServer.
    /// </summary>
    public void CreateDatabase(String name) throws CouchException
    {
        try
        {
            Request().Path(name).Put().Check("Failed to create database");
        }
        catch (Exception e)
        {
            throw CouchException.Create("Failed to create database", e);
        }
    }

    public void DeleteAllDatabases() throws CouchException, JSONException
    {
        DeleteDatabases(".*");
    }

    public void DeleteDatabases(String regExp) throws CouchException, JSONException
    {
    	Pattern reg = Pattern.compile(regExp);
        
        List<String> list = this.GetDatabaseNames();
        
        for (String name : list)
        {
            if (reg.matcher(name).matches())
            {
                DeleteDatabase(name);
            }
        }
    }

    public void DeleteDatabase(String name) throws CouchException
    {
        try
        {
            Request().Path(name).Delete().Check("Failed to delete database");
        }
        catch (Exception e)
        {
            throw new CouchException("Failed to delete database", e);
        }
    }

    public List<String> GetDatabaseNames() 
    {

    	String list = Request().Path("_all_dbs").Get().String();
    	JSONArray dbs = new JSONArray();
    	try {
    		dbs = new JSONArray(list);
    	} catch (JSONException e) { }
    	
    	List<String> result = new ArrayList<String>(); 
    	for (int i = 0; i< dbs.length(); i++) {
    		result.add(dbs.optString(i));
    	}
    	return result;
    }

}

