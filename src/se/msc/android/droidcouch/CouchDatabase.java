package se.msc.android.droidcouch;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

import org.apache.http.client.ClientProtocolException;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class CouchDatabase {
    private String name;
    public List<CouchDesignDocument> DesignDocuments = new ArrayList<CouchDesignDocument>();

    public CouchDatabase()
    {
        name = "default";
    }

    public CouchDatabase(CouchServer server)
    {
    	this();
        Server = server;
    }

    public CouchDatabase(String name, CouchServer server)
    {
        name = name;
        Server = server;
    }

    public String Name()
    {
    	if (Server == null)
    		return name;
    	return Server.DatabasePrefix + name;
	}
    
    public void Name(String value)
	{
    	name = value;
	}

    public CouchServer Server;
    
//    public Boolean RunningOnMono()
//    {
//        return Server.RunningOnMono;
//    }

    public CouchDesignDocument NewDesignDocument(String aName)
    {
    	CouchDesignDocument newDoc = new CouchDesignDocument(aName, this);
        DesignDocuments.add(newDoc);
        return newDoc;
    }

	/// <summary>
	/// Only to be used when developing.
	/// </summary>
	public CouchViewDefinition NewTempView(String designDoc, String viewName, String mapText) throws Exception
    {
		CouchDesignDocument doc = NewDesignDocument(designDoc);
        CouchViewDefinition view = doc.AddView(viewName, "function (doc) {" + mapText + "}");
        doc.Synch();
        return view;
    }
			
    /// <summary>
    /// Currently the logic is that the code is always the master.
    /// And we also do not remove design documents in the database that
    /// we no longer have in code.
    /// </summary>
    public void SynchDesignDocuments()
    {
        for (CouchDesignDocument doc: DesignDocuments)
        {
            try {
				doc.Synch();
			} catch (CouchException e) {
			}
        }
    }

    public CouchRequest Request()
    {
        return new CouchRequest(this);
    }

    public CouchRequest Request(String path)
    {
        return (new CouchRequest(this)).Path(path);
    }

    public int CountDocuments()
    {
        return (Request().Parse()).optInt("doc_count");
    }

    public CouchRequest RequestAllDocuments()
    {
        return Request("_all_docs");
    }

    /// <summary>
    /// Return all documents in the database as CouchJsonDocuments.
    /// This method is only practical for testing purposes.
    /// </summary>
    /// <returns>A list of all documents.</returns>
    public List<CouchJsonDocument> GetAllDocuments() throws JSONException, CouchException
    {
        return QueryAllDocuments().IncludeDocuments().GetResult().Documents(CouchJsonDocument.class);
    }

    /// <summary>
    /// Return all documents in the database using a supplied
    /// document type implementing ICouchDocument.
    /// This method is only practical for testing purposes.
    /// </summary>
    /// <typeparam name="T">The document type to use.</typeparam>
    /// <returns>A list of all documents.</returns>
    public <T extends ICouchDocument> List<T> GetAllDocuments(Class<T> c) throws JSONException, CouchException
    { 
        return QueryAllDocuments().IncludeDocuments().GetResult().Documents(c);
    }

    /// <summary>
    /// Return all documents in the database, but only with id and revision.
    /// CouchDocument does not contain the actual content.
    /// </summary>
    /// <returns>List of documents</returns>
    public List<CouchDocument> GetAllDocumentsWithoutContent() throws JSONException, CouchException
    {
        QueryAllDocuments().GetResult().ValueDocuments(CouchDocument.class);

        List<CouchDocument> list = new ArrayList<CouchDocument>();
        JSONObject json = RequestAllDocuments().Parse();
        JSONArray arr = json.getJSONArray("rows");
        for (int i = 0; i< arr.length(); i++)
        {
        	JSONObject obj = arr.getJSONObject(i);
            list.add(new CouchDocument(obj.getString("id"), 
            		obj.getJSONObject("value").getString("rev")));
        }
        return list;
    }

    /// <summary>
    /// Initialize CouchDB database by saving new or changed design documents into it.
    /// Override if needed in subclasses.
    /// </summary>
    public void Initialize() throws Exception
    {
        SynchDesignDocuments();
    }

    public boolean Exists()
    {
        return Server.HasDatabase(name);
    }

    /// <summary>
    /// Check first if database exists, and if it does not - create it and initialize it.
    /// </summary>
    public void Create() throws Exception
    {
        if (!Exists())
        {
            Server.CreateDatabase(name);
            Initialize();
        }
    }

    public void Delete() throws CouchException
    {
        if (Exists())
        {
            Server.DeleteDatabase(name);
        }
    }

    /// <summary>
    /// Write a document given as plain JSON and a document id. A document may already exist in db and will then be overwritten.
    /// </summary>
    /// <param name="json">Document as a JSON string</param>
    /// <param name="documentId">Document identifier</param>
    /// <returns>A new CouchJsonDocument</returns>
    public ICouchDocument WriteDocument(String json, String documentId) throws JSONException, CouchException
    {
        return WriteDocument(new CouchJsonDocument(json, documentId));
    }

    /// <summary>
    /// Write a CouchDocument or ICouchDocument, it may already exist in db and will then be overwritten.
    /// </summary>
    /// <param name="document">Couch document</param>
    /// <returns>Couch Document with new Rev set.</returns>
    /// <remarks>This relies on the document to already have an id.</remarks>
    public ICouchDocument
        WriteDocument(ICouchDocument document) throws CouchException
    {
        return WriteDocument(document, false);
    }

    /// <summary>
    /// This is a convenience method that creates or writes a ICouchDocument depending on if
    /// it has an id or not. If it does not have an id we create the document and let CouchDB allocate
    /// an id. If it has an id we use WriteDocument which will overwrite the document in CouchDB.
    /// </summary>
    /// <param name="document">Couch document</param>
    /// <returns>Couch Document with new Rev set and possibly an Id set.</returns>
    public ICouchDocument SaveDocument(ICouchDocument document) throws CouchException
    {
        if (document.Id() == null)
        {
            return CreateDocument(document);
        }
        return WriteDocument(document);
    }

    /// <summary>
    /// Write a CouchDocument or ICouchDocument, it may already exist in db and will then be overwritten.
    /// </summary>
    /// <param name="document">Couch document</param>
    /// <param name="batch">True if we don't want to wait for flush (commit).</param>
    /// <returns>Couch Document with new Rev set.</returns>
    /// <remarks>This relies on the document to already have an id.</remarks>
    public ICouchDocument WriteDocument(ICouchDocument document, boolean batch) throws CouchException
    {
        if (document.Id() == null)
        {
            throw CouchException.Create(
                "Failed to write document using PUT because it lacks an id, use CreateDocument instead to let CouchDB generate an id");
        }
        JSONObject result =
            Request(document.Id()).Query(batch ? "?batch=ok" : null).Data(CouchDocument.WriteJson(document)).Put().Check("Failed to write document").Result();
        document.Id(result.optString("id")); // Not really neeed
        document.Rev(result.optString("rev"));
        return document;
    }

    /// <summary>
    /// Add an attachment to an existing ICouchDocument, it may already exist in db and will then be overwritten.
    /// </summary>
    /// <param name="document">Couch document</param>
    /// <param name="attachment">Binary data as string</param>
    /// <param name="mimeType">The MIME type for the attachment.</param>
    /// <returns>The document.</returns>
    /// <remarks>This relies on the document to already have an id.</remarks>
    public ICouchDocument WriteAttachment(ICouchDocument document, String attachment, String mimeType) throws CouchException
    {
        if (document.Id() == null)
        {
            throw CouchException.Create("Failed to add attachment to document using PUT because it lacks an id");
        }

        JSONObject result =
            Request(document.Id() + "/attachment").Query("?rev=" + document.Rev()).Data(attachment).MimeType(mimeType).Put().Check("Failed to write attachment")
                .Result();
        document.Id(result.optString("id")); // Not really neeed
        document.Rev(result.optString("rev"));

        return document;
    }

    /// <summary>
    /// Read a ICouchDocument with an id even if it has not changed revision.
    /// </summary>
    /// <param name="document">Document to fill.</param>
    public void ReadDocument(ICouchDocument document) 
    {
    	try {
    		document.ReadJson(ReadDocument(document.Id()));
    	} catch (Exception e) {
       	
        }
    }

    /// <summary>
    /// Read the attachment for an ICouchDocument.
    /// </summary>
    /// <param name="document">Document to read.</param>
    public String ReadAttachment(ICouchDocument document)
    {
        return ReadAttachment(document.Id());
    }

    /// <summary>
    /// First use HEAD to see if it has indeed changed.
    /// </summary>
    /// <param name="document">Document to fill.</param>
    public void FetchDocumentIfChanged(ICouchDocument document) throws Exception
    {
        if (HasDocumentChanged(document))
        {
            ReadDocument(document);
        }
    }

    /// <summary>
    /// Read a CouchDocument or ICouchDocument, this relies on the document to obviously have an id.
    /// We also check the revision so that we can avoid parsing JSON if the document is unchanged.
    /// </summary>
    /// <param name="document">Document to fill.</param>
    public void ReadDocumentIfChanged(ICouchDocument document) throws Exception
    {
        JSONObject result = Request(document.Id()).Etag(document.Rev()).Parse();
        if (result == null)
        {
            return;
        }
        document.ReadJson(result);
    }

    /// <summary>
    /// Read a couch document given an id, this method does not have enough information to do caching.
    /// </summary>
    /// <param name="documentId">Document identifier</param>
    /// <returns>Document Json as JSONObject</returns>
    public JSONObject ReadDocument(String documentId)
    {
            return Request(documentId).Parse();
//            throw CouchException.Create("Failed to read document", e);
        
    }

    /// <summary>
    /// Read a couch document given an id, this method does not have enough information to do caching.
    /// </summary>
    /// <param name="documentId">Document identifier</param>
    /// <returns>Document Json as string</returns>
    public String ReadDocumentString(String documentId)
    {
    	return Request(documentId).String();
           // throw CouchException.Create("Failed to read document: " + e.Message, e);
    }

    /// <summary>
    /// Read a couch attachment given a document id, this method does not have enough information to do caching.
    /// </summary>
    /// <param name="documentId">Document identifier</param>
    /// <returns>Document attachment</returns>
    public String ReadAttachment(String documentId)
    {
            return Request(documentId + "/attachment").String();
            //throw CouchException.Create("Failed to read document: " + e.Message, e);
    }

    /// <summary>
    /// Create a CouchDocument given JSON as a string. Uses POST and CouchDB will allocate a new id.
    /// </summary>
    /// <param name="json">Json data to store.</param>
    /// <returns>Couch document with data, id and rev set.</returns>
    /// <remarks>POST which may be problematic in some environments.</remarks>
    public CouchJsonDocument CreateDocument(String json) throws CouchException
    {
        return (CouchJsonDocument) CreateDocument(new CouchJsonDocument(json));
    }

    /// <summary>
    /// Create a given ICouchDocument in CouchDB. Uses POST and CouchDB will allocate a new id and overwrite any existing id.
    /// </summary>
    /// <param name="document">Document to store.</param>
    /// <returns>Document with Id and Rev set.</returns>
    /// <remarks>POST which may be problematic in some environments.</remarks>
    public ICouchDocument CreateDocument(ICouchDocument document) throws CouchException
    {
		try {
  
			JSONObject result = Request().Data(CouchDocument.WriteJson(document)).Post().Check("Failed to create document").Result();
			document.Id(result.getString("id"));
			document.Rev(result.getString("rev"));
			return document;
		} catch (JSONException e) {
			throw CouchException.Create("Failed to create document", e);
		}
    }

    /// <summary>
    /// Create or update a list of ICouchDocuments in CouchDB. Uses POST and CouchDB will 
    /// allocate new ids if the documents lack them.
    /// </summary>
    /// <param name="documents">List of documents to store.</param>
    /// <remarks>POST may be problematic in some environments.</remarks>
    public void SaveDocuments(List<ICouchDocument> documents, boolean allOrNothing) throws CouchException
    {
    	CouchBulkDocuments bulk = new CouchBulkDocuments(documents);
    	String allornothing = (allOrNothing) ? "true" : "false";
        JSONObject result =
            Request("_bulk_docs").Data(CouchDocument.WriteJson(bulk)).Query("?all_or_nothing=" + allornothing)
            .PostJson().Parse();
        try 
        {
	        for (int i = 0; i < documents.size(); i++)
	        {
	            documents.get(i).Id(
	            		result.getJSONObject(String.valueOf(i)).getString("id"));
	            documents.get(i).Rev(
	            		result.getJSONObject(String.valueOf(i)).getString("rev"));
	        } 
        }
        catch(Exception e){
            throw CouchException.Create("Failed to create bulk documents", e);
        }
    }

    /// <summary>
    /// Create or updates documents in bulk fashion, chunk wise. Optionally access given view 
    /// after each chunk to trigger reindexing.
    /// </summary>
    /// <param name="documents">List of documents to store.</param>
    /// <param name="chunkCount">Number of documents to store per "POST"</param>
    /// <param name="views">List of views to touch per chunk.</param>
    public void SaveDocuments(List<ICouchDocument> documents, int chunkCount, List<CouchViewDefinition> views, boolean allOrNothing) throws CouchException
    {
        ArrayList<ICouchDocument> chunk = new ArrayList<ICouchDocument>(chunkCount);
        int counter = 0;

        for (ICouchDocument doc: documents)
        {
            // Do we have a chunk ready to create?
            if (counter == chunkCount)
            {
                counter = 0;
                SaveDocuments(chunk, allOrNothing);
                TouchViews(views);
                /* Skipping separate thread for now, ASP.Net goes bonkers...
                (new Thread(
                    () => GetView<CouchPermanentViewResult>(designDocumentName, viewName, ""))
                {
                    Name = "View access in background", Priority = ThreadPriority.BelowNormal
                }).Start(); */

                chunk = new ArrayList<ICouchDocument>(chunkCount);
            }
            counter++;
            chunk.add(doc);
        }

        SaveDocuments(chunk, allOrNothing);
        TouchViews(views);
    }

    public void TouchViews(List<CouchViewDefinition> views) throws CouchException
    {
        //var timer = new Stopwatch();
        if (views != null)
        {
            for(CouchViewDefinition view: views)
            {
                if (view != null)
                {
                    //timer.Reset();
                    //timer.Start();
                    view.Touch();
                    //timer.Stop();
                    //Server.Debug("Update view " + view.Path() + ":" + timer.ElapsedMilliseconds + " ms");
                }
            }
        }
    }

    /// <summary>
    /// Create documents in bulk fashion, chunk wise. 
    /// </summary>
    /// <param name="documents">List of documents to store.</param>
    /// <param name="chunkCount">Number of documents to store per "POST"</param>
    public void SaveDocuments(List<ICouchDocument> documents, int chunkCount, boolean allOrNothing) throws CouchException
    {
        SaveDocuments(documents, chunkCount, null, allOrNothing);
    }

    /// <summary>
    /// Get multiple documents.
    /// </summary>
    /// <param name="documentIds">List of documents to get.</param>
    public <T extends ICouchDocument> List<T> GetDocuments(Class<T> c, List<String> documentIds)
    {
        return GetDocuments(c, (String[])documentIds.toArray());
    }

    public List<CouchJsonDocument> GetDocuments(List<String> documentIds)
    {
        return GetDocuments(CouchJsonDocument.class,documentIds);
    }

    public List<CouchJsonDocument> GetDocuments(String[] documentIds) 
    {
        return GetDocuments(CouchJsonDocument.class,documentIds);
    }

    public <T extends ICouchDocument> List<T> GetDocuments(Class<T> c, String[] documentIds) 
    {
    	CouchBulkKeys bulk = new CouchBulkKeys(documentIds);
        try {
			return QueryAllDocuments().Data(CouchDocument.WriteJson(bulk)).IncludeDocuments().GetResult().Documents(c);
		} catch (CouchException e) {
			return new ArrayList<T>();
		}
    }

    public <T extends ICouchDocument> T GetDocument(Class<T> c, String documentId)
    {
    	ICouchDocument doc;
		try {
			doc = c.newInstance();
		} catch (Exception e1) {
			return null;
		}
    	doc.Id(documentId);
        ReadDocument(doc);
        return (T)doc;
    }

    public CouchJsonDocument GetDocument(String documentId) throws CouchException
    {
        try
        {
            try
            {
                return new CouchJsonDocument(Request(documentId).Parse());
            }
            catch (Exception e)
            {
                throw CouchException.Create("Failed to get document", e);
            }
        }
        catch (CouchNotFoundException e)
        {
            return null;
        }
    }
    /// <summary>
    /// Query a view by name (that we know exists in CouchDB). This method then creates
    /// a CouchViewDefinition on the fly. Better to use existing CouchViewDefinitions.
    /// </summary>
    public CouchQuery Query(String designName, String viewName)
    {
        return Query(new CouchViewDefinition(viewName, NewDesignDocument(designName)));
    }

    public CouchQuery Query(CouchViewDefinition view)
    {
        return new CouchQuery(view);
    }


/*    public CouchLuceneQuery Query(CouchLuceneViewDefinition view)
    {
        return new CouchLuceneQuery(view);
    }
*/
    public CouchQuery QueryAllDocuments()
    {
        return Query(null, "_all_docs");
    }

    public void TouchView(String designDocumentId, String viewName) throws CouchException
    {
        Query(designDocumentId, viewName).Limit(0).GetResult();
    }

    public void DeleteDocument(ICouchDocument document) throws CouchException
    {
        DeleteDocument(document.Id(), document.Rev());
    }

    public ICouchDocument DeleteAttachment(ICouchDocument document) throws CouchException
    {
        JSONObject result = Request(document.Id() + "/attachment").Query("?rev=" + document.Rev()).Delete().Check("Failed to delete attachment").Result();
        document.Id(result.optString("id")); // Not really neeed
        document.Rev(result.optString("rev"));
        return document;
    }

    public void DeleteAttachment(String id, String rev) throws CouchException
    {
        Request(id + "/attachment").Query("?rev=" + rev).Delete().Check("Failed to delete attachment");
    }

    public void DeleteDocument(String id, String rev) throws CouchException
    {
        Request(id).Query("?rev=" + rev).Delete().Check("Failed to delete document");
    }

    /// <summary>
    /// Delete documents in bulk fashion.
    /// </summary>
    /// <param name="documents">List of documents to delete.</param>
    public void DeleteDocuments(List<ICouchDocument> documents) throws CouchException
    {
        DeleteDocuments(new CouchBulkDeleteDocuments(documents));
    }

    /// <summary>
    /// Delete documents in key range. This method needs to retrieve
    /// revisions and then use them to post a bulk delete. Couch can not
    /// delete documents without being told about their revisions.
    /// </summary>
    public void DeleteDocuments(String startKey, String endKey) throws CouchException
    {
        List<CouchQueryDocument> docs = QueryAllDocuments().StartKey(startKey).EndKey(endKey).GetResult().RowDocuments();
        DeleteDocuments((ICouchDocument[] )docs.toArray());
    }

    /// <summary>
    /// Delete documents in bulk fashion.
    /// </summary>
    /// <param name="documents">Array of documents to delete.</param>
    public void DeleteDocuments(ICouchDocument[] documents) throws CouchException
    {
        DeleteDocuments(new CouchBulkDeleteDocuments(documents));
    }

    /// <summary>
    /// Delete documents in bulk fashion.
    /// </summary>
    public void DeleteDocuments(ICanJson bulk) throws CouchException
    {
        try
        {
            JSONObject result = Request("_bulk_docs").Data(CouchDocument.WriteJson(bulk)).PostJson().Parse();
         
            for (Iterator i = result.keys(); i.hasNext(); )
            {
            	JSONObject doc = (JSONObject)i.next();
                //documents[i].id = (result[i])["id"].Value<String>();
                //documents[i].rev = (result[i])["rev"].Value<String>();
                if (doc.get("error") != null)
                {
                	
                    throw CouchException.Create(String.format(
                        "Document with id %s was not deleted: %s: %s",
                        doc.getString("id"), doc.getString("error"),
                        doc.getString("reason")));
                }
            }
        }
        catch (Exception e)
        {
            throw CouchException.Create("Failed to bulk delete documents", e);
        }
    }

    public boolean HasDocument(ICouchDocument document)
    {
        return HasDocument(document.Id());
    }

    public boolean HasAttachment(ICouchDocument document)
    {
        return HasAttachment(document.Id());
    }

    public boolean HasDocumentChanged(ICouchDocument document) throws ClientProtocolException, IOException
    {
        return HasDocumentChanged(document.Id(), document.Rev());
    }

    public boolean HasDocumentChanged(String documentId, String rev) throws ClientProtocolException, IOException
    {
        return Request(documentId).Head().Send().Etag() != rev;
    }

    public boolean HasDocument(String documentId)
    {
            try {
				Request(documentId).Head().Send();
			} catch (ClientProtocolException e) {
				
				 return false;
			} catch (IOException e) {
			
				 return false;
			}
            return true;
    }

    public boolean HasAttachment(String documentId)
    {

            try {
				Request(documentId + "/attachment").Head().Send();
			} catch (ClientProtocolException e) {
				 return false;
			} catch (IOException e) {
				 return false;
			}
            return true;
     
    }
}
