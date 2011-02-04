package se.msc.android.droidcouch;

import org.json.JSONObject;

import android.app.Activity;
import android.os.Bundle;
import android.widget.TextView;

public class TestClient extends Activity {

    String HOST = "ADD_YOUR_OWN_COUCHDB_SERVER_HERE";

    String DBNAME = "hackathon_2";
    String TEST_DOC_ID = "test_document_1";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        TextView view = new TextView(this);
        view.append("Running tests...\n\n");
        setContentView(view);

        try {
            testDatabaseDoesNotExist();
            testCreateDatabase();
            testGetDatabaseInfo();
            testCreateDocument();
            testGetDocument();
            testGetAllDocuments();
            testUpdateDocument();
            testDeleteDocument();
            testDeleteDatabase();
            view.append("All tests passed!");
        } catch (Exception e) {
            String trace = DroidCouch.getStacktrace(e);
            view.append(trace);
        }
    }

    public void testCreateDatabase() throws Exception {
        boolean result = DroidCouch.createDatabase(HOST, DBNAME);
        shouldBeTrue(result, "Database could not be created!");
    }

    public void testDatabaseDoesNotExist() throws Exception {
        String couchUrl = HOST + DBNAME;
        JSONObject responseObject = DroidCouch.get(couchUrl);
        String error = responseObject.getString("error");
        String reason = responseObject.getString("reason");
        shouldBeTrue(error.equals("not_found"), "Incorrect message recieved");
        shouldBeTrue(reason.equals("no_db_file"), "Incorrect message recieved");
    }

    public void testDeleteDatabase() throws Exception {
        boolean result = DroidCouch.deleteDatabase(HOST, DBNAME);
        shouldBeTrue(result, "Database could not be deleted!");
    }

    public void testGetDatabaseInfo() throws Exception {
        String couchUrl = HOST + DBNAME;
        JSONObject responseObject = DroidCouch.get(couchUrl);
        String actualDbName = responseObject.getString("db_name");
        shouldBeTrue(DBNAME.equals(actualDbName), "Incorrect message recieved");
    }

    public void testCreateDocument() throws Exception {
        JSONObject json = new JSONObject().put("test_key_1", "test_value_1")
                .put("test_key_2", "test_value_2");
        String revision = DroidCouch.createDocument(HOST, DBNAME, TEST_DOC_ID,
                json);
        shouldBeTrue(null != revision, "Document has no revision number");
    }

    public void testGetDocument() throws Exception {
        JSONObject doc = DroidCouch.getDocument(HOST, DBNAME, TEST_DOC_ID);
        shouldBeTrue(null != doc, "Document is null");
        shouldBeTrue(doc.getString("test_key_2").equals("test_value_2"),
                "Wrong value");
    }

    public void testDeleteDocument() throws Exception {
        boolean result = DroidCouch.deleteDocument(HOST, DBNAME, TEST_DOC_ID);
        shouldBeTrue(result, "Document could not be deleted!");
    }

    public void testUpdateDocument() throws Exception {
        JSONObject doc = DroidCouch.getDocument(HOST, DBNAME, TEST_DOC_ID);
        String oldRevision = doc.getString("_rev");
        doc.put("test_key_1", "updated_test_value_1");
        String newRevision = DroidCouch.updateDocument(HOST, DBNAME, doc);
        shouldBeTrue(null != newRevision, "Revision is null");
        shouldBeTrue(!oldRevision.equals(newRevision),
                "Revision has not changed!");
        JSONObject newDoc = DroidCouch.getDocument(HOST, DBNAME, TEST_DOC_ID);
        shouldBeTrue(newDoc.getString("test_key_1").equals(
                "updated_test_value_1"), "Wrong value");
    }

    public void testGetAllDocuments() throws Exception {
        JSONObject responseObject = DroidCouch.getAllDocuments(HOST, DBNAME);
        shouldBeTrue(responseObject.has("total_rows"),
                "Incorrect message recieved");
    }

 
    public void testPutAttachment() throws Exception {
    }

    public void testGetAttachment() throws Exception {
    }

    private void shouldBeTrue(boolean mustBeTrue, String message)
            throws Exception {
        if (!mustBeTrue)
            throw new Exception("Assert failed: " + message);
    }
}