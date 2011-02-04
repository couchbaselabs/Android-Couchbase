package se.msc.android.droidcouch;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.client.methods.HttpPut;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.json.JSONObject;
import android.util.Log;

public class DroidCouch {
    static final String TAG = "DroidCouchLibrary";

    public static String convertStreamToString(InputStream is) {
        /*
         * To convert the InputStream to String we use the
         * BufferedReader.readLine() method. We iterate until the BufferedReader
         * return null which means there's no more data to read. Each line will
         * appended to a StringBuilder and returned as String.
         * 
         * (c) public domain:
         * http://senior.ceng.metu.edu.tr/2009/praeda/2009/01/
         * 11/a-simple-restful-client-at-android/
         */
        BufferedReader reader = new BufferedReader(new InputStreamReader(is), 8192);
        StringBuilder sb = new StringBuilder();

        String line = null;
        try {
            while ((line = reader.readLine()) != null) {
                sb.append(line + "\n");
            }
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                is.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return sb.toString();
    }

    public static boolean createDatabase(String hostUrl, String databaseName) {
        try {
            HttpPut httpPutRequest = new HttpPut(hostUrl + databaseName);
            JSONObject jsonResult = sendCouchRequest(httpPutRequest);
            return jsonResult.getBoolean("ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @return the revision id of the created document
     */
    public static String createDocument(String hostUrl, String databaseName,
            String docId, JSONObject jsonDoc) {
        try {
            HttpPut httpPutRequest = new HttpPut(hostUrl + databaseName + "/"
                    + docId);
            StringEntity body = new StringEntity(jsonDoc.toString(), "utf8");
            httpPutRequest.setEntity(body);
            httpPutRequest.setHeader("Accept", "application/json");
            httpPutRequest.setHeader("Content-type", "application/json");
            JSONObject jsonResult = sendCouchRequest(httpPutRequest);
            if (!jsonResult.getBoolean("ok")) {
                return null; // Not ok!
            }
            return jsonResult.getString("rev");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean deleteDatabase(String hostUrl, String databaseName) {
        try {
            HttpDelete httpDeleteRequest = new HttpDelete(hostUrl
                    + databaseName);
            JSONObject jsonResult = sendCouchRequest(httpDeleteRequest);
            return jsonResult.getBoolean("ok");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @return true if document successfully deleted
     */
    public static boolean deleteDocument(String hostUrl, String databaseName,
            String docId) {
        try {
            JSONObject jsonDoc = getDocument(hostUrl, databaseName, docId);
            return deleteDocument(hostUrl, databaseName, docId, jsonDoc
                    .getString("_rev"));
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    /**
     * @return true if document successfully deleted
     */
    public static boolean deleteDocument(String hostUrl, String databaseName,
            String docId, String rev) {
        try {
            String url = hostUrl + databaseName + "/" + docId + "?rev=" + rev;
            HttpDelete httpDeleteRequest = new HttpDelete(url);
            JSONObject jsonResult = sendCouchRequest(httpDeleteRequest);
            if (jsonResult != null) {
                return jsonResult.getBoolean("ok");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return false;
    }

    public static JSONObject get(String url) {
        DefaultHttpClient httpclient = new DefaultHttpClient();
        // Prepare a request object
        HttpGet httpget = new HttpGet(url);
        // Execute the request
        HttpResponse response;
        JSONObject json = null;
        try {
            response = httpclient.execute(httpget);
            // Examine the response status
            Log.i(TAG, response.getStatusLine().toString());

            // Get hold of the response entity
            HttpEntity entity = response.getEntity();
            // If the response does not enclose an entity, there is no need
            // to worry about connection release

            if (entity != null) {
                // A Simple JSON Response Read
                InputStream instream = entity.getContent();
                String result = convertStreamToString(instream);
                // Log.i(TAG, result);

                // A Simple JSONObject Creation
                json = new JSONObject(result);
                Log.i(TAG, json.toString(2));
                // Closing the input stream will trigger connection release
                instream.close();
            }
        } catch (Exception e) {
            // String timestamp =
            // TimestampFormatter.getInstance().getTimestamp();
            Log.e(TAG, getStacktrace(e));
            return null;
        }
        return json;
    }

    /**
     * @return the Json document
     */
    public static JSONObject getDocument(String hostUrl, String databaseName,
            String docId) {
        try {
            HttpGet httpGetRequest = new HttpGet(hostUrl + databaseName + "/"
                    + docId);
            JSONObject jsonResult = sendCouchRequest(httpGetRequest);
            if (jsonResult != null) {
                return jsonResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getStacktrace(Throwable e) {
        final Writer trace = new StringWriter();
        e.printStackTrace(new PrintWriter(trace));
        return trace.toString();
    }

    /**
     * @return a Json object, null on error
     */
    private static JSONObject sendCouchRequest(HttpUriRequest request) {
        try {
            HttpResponse httpResponse = (HttpResponse) new DefaultHttpClient()
                    .execute(request);
            HttpEntity entity = httpResponse.getEntity();
            if (entity != null) {
                // Read the content stream
                InputStream instream = entity.getContent();
                // Convert content stream to a String
                String resultString = convertStreamToString(instream);
                instream.close();
                // Transform the String into a JSONObject
                JSONObject jsonResult = new JSONObject(resultString);
                return jsonResult;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return the revision id of the updated document
     */
    public static String updateDocument(String hostUrl, String databaseName,
            JSONObject jsonDoc) {
        try {
            String docId = jsonDoc.getString("_id");
            HttpPut httpPutRequest = new HttpPut(hostUrl + databaseName + "/"
                    + docId);
            StringEntity body = new StringEntity(jsonDoc.toString(), "utf8");
            httpPutRequest.setEntity(body);
            httpPutRequest.setHeader("Accept", "application/json");
            httpPutRequest.setHeader("Content-type", "application/json");
            JSONObject jsonResult = sendCouchRequest(httpPutRequest);
            if (!jsonResult.getBoolean("ok")) {
                return null; // Not ok!
            }
            return jsonResult.getString("rev");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    /**
     * @return a Json document with all documents in the database
     */
    public static JSONObject getAllDocuments(String hostUrl, String databaseName) {
        try {
            String url = hostUrl + databaseName
                    + "/_all_docs?include_docs=true";
            HttpGet httpGetRequest = new HttpGet(url);
            JSONObject jsonReceive = sendCouchRequest(httpGetRequest);
            if (jsonReceive != null) {
                return jsonReceive;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    // public static JSONObject SendHttpPut(String url, JSONObject jsonObjSend)
    // {
    // JSONObject jsonObjRecv = new JSONObject();
    // DefaultHttpClient httpclient = new DefaultHttpClient();
    // try {
    // String current_id = jsonObjSend.getString("_id");
    // HttpPut httpPutRequest = new HttpPut(url + "/" + current_id);
    // StringEntity se;
    // se = new StringEntity(jsonObjSend.toString());
    // httpPutRequest.setEntity(se);
    // httpPutRequest.setHeader("Accept", "application/json");
    // httpPutRequest.setHeader("Content-type", "application/json");
    // long t = System.currentTimeMillis();
    // HttpResponse response = (HttpResponse) httpclient
    // .execute(httpPutRequest);
    // Log.i(TAG, "HTTPResponse received in ["
    // + (System.currentTimeMillis() - t) + "ms]");
    // // Get hold of the response entity (-> the data):
    // HttpEntity entity = response.getEntity();
    // if (entity != null) {
    // // Read the content stream
    // InputStream instream = entity.getContent();
    // Header contentEncoding = response
    // .getFirstHeader("Content-Encoding");
    // if (contentEncoding != null
    // && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
    // instream = new GZIPInputStream(instream);
    // }
    // // convert content stream to a String
    // String resultString = convertStreamToString(instream);
    // instream.close();
    // // Transform the String into a JSONObject
    // jsonObjRecv = new JSONObject(resultString);
    // // Raw DEBUG output of our received JSON object:
    // Log.i(TAG, jsonObjRecv.toString(2));
    // }
    // } catch (Exception e) {
    // // More about HTTP exception handling in another tutorial.
    // // For now we just print the stack trace.
    // e.printStackTrace();
    // }
    // return jsonObjRecv;
    // }
    //
    // public static JSONObject SendHttpPutAttachment(String url,
    // JSONObject sender, String attachment, String data, String mimetype) {
    // JSONObject jsonObjRecv = new JSONObject();
    // DefaultHttpClient httpclient = new DefaultHttpClient();
    // try {
    // String id = sender.getString("_id");
    // String rev = sender.getString("_rev");
    // HttpPut httpPutRequest = new HttpPut(url + "/" + id + "/"
    // + attachment + "?rev=" + rev);
    // StringEntity se;
    // se = new StringEntity(data);
    // httpPutRequest.setEntity(se);
    // // httpPutRequest.setHeader("Content-length",
    // // Integer.toString(attachment.length()));
    // httpPutRequest.setHeader("Content-type", mimetype);
    // long t = System.currentTimeMillis();
    // HttpResponse response = (HttpResponse) httpclient
    // .execute(httpPutRequest);
    // Log.i(TAG, "HTTPResponse received in ["
    // + (System.currentTimeMillis() - t) + "ms]");
    // // Get hold of the response entity (-> the data):
    // HttpEntity entity = response.getEntity();
    // if (entity != null) {
    // // Read the content stream
    // InputStream instream = entity.getContent();
    // Header contentEncoding = response
    // .getFirstHeader("Content-Encoding");
    // if (contentEncoding != null
    // && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
    // instream = new GZIPInputStream(instream);
    // }
    // // convert content stream to a String
    // String resultString = convertStreamToString(instream);
    // instream.close();
    // // Transform the String into a JSONObject
    // jsonObjRecv = new JSONObject(resultString);
    // // Raw DEBUG output of our received JSON object:
    // Log.i(TAG, jsonObjRecv.toString(2));
    // }
    // } catch (Exception e) {
    // // More about HTTP exception handling in another tutorial.
    // // For now we just print the stack trace.
    // e.printStackTrace();
    // }
    // return jsonObjRecv;
    // }

    // public static JSONObject SendHttpPost(String url, JSONObject jsonObjSend)
    // {
    // JSONObject jsonObjRecv = new JSONObject();
    // DefaultHttpClient httpclient = new DefaultHttpClient();
    // try {
    //
    // HttpPost httpPostRequest = new HttpPost(url);
    // StringEntity se;
    // se = new StringEntity(jsonObjSend.toString());
    // // Set HTTP parameters
    // httpPostRequest.setEntity(se);
    // httpPostRequest.setHeader("Accept", "application/json");
    // httpPostRequest.setHeader("Content-type", "application/json");
    // long t = System.currentTimeMillis();
    // HttpResponse response = (HttpResponse) httpclient
    // .execute(httpPostRequest);
    // Log.i(TAG, "HTTPResponse received in ["
    // + (System.currentTimeMillis() - t) + "ms]");
    // // Get hold of the response entity (-> the data):
    // HttpEntity entity = response.getEntity();
    // if (entity != null) {
    // // Read the content stream
    // InputStream instream = entity.getContent();
    // Header contentEncoding = response
    // .getFirstHeader("Content-Encoding");
    // if (contentEncoding != null
    // && contentEncoding.getValue().equalsIgnoreCase("gzip")) {
    // instream = new GZIPInputStream(instream);
    // }
    // // convert content stream to a String
    // String resultString = convertStreamToString(instream);
    // instream.close();
    // // Transform the String into a JSONObject
    // jsonObjRecv = new JSONObject(resultString);
    // // Raw DEBUG output of our received JSON object:
    // Log.i(TAG, jsonObjRecv.toString(2));
    // }
    // } catch (Exception e) {
    // // More about HTTP exception handling in another tutorial.
    // // For now we just print the stack trace.
    // e.printStackTrace();
    // }
    // return jsonObjRecv;
    // }
    //
    // public static JSONObject DeleteHttpDelete(String url) {
    // DefaultHttpClient httpclient = new DefaultHttpClient();
    // // Prepare a request object
    // HttpDelete httpDelete = new HttpDelete(url);
    // // Execute the request
    // HttpResponse response;
    // JSONObject json = null;
    // try {
    // response = httpclient.execute(httpDelete);
    // // Examine the response status
    // Log.i(TAG, response.getStatusLine().toString());
    //
    // // Get hold of the response entity
    // HttpEntity entity = response.getEntity();
    // // If the response does not enclose an entity, there is no need
    // // to worry about connection release
    //
    // if (entity != null) {
    // // A Simple JSON Response Read
    // InputStream instream = entity.getContent();
    // String result = convertStreamToString(instream);
    // Log.i(TAG, result);
    //
    // // A Simple JSONObject Creation
    // json = new JSONObject(result);
    // Log.i(TAG, json.toString(2));
    //
    // // Closing the input stream will trigger connection release
    // instream.close();
    // }
    // } catch (ClientProtocolException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (IOException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // } catch (JSONException e) {
    // // TODO Auto-generated catch block
    // e.printStackTrace();
    // }
    // return json;
    // }

    // public static JSONObject getSampleData(String url, String status) {
    // // JSON object to hold the information, which is sent to the server
    // JSONObject jsonObjSend = new JSONObject();
    //
    // try {
    // // Add key/value pairs
    // Random generator = new Random();
    // int newKey = generator.nextInt(Integer.MAX_VALUE);
    // jsonObjSend.put("_id", Integer.toString(newKey));
    // jsonObjSend.put("status", status);
    // jsonObjSend.put("customer", "Imerica");
    //
    // // Add nested JSONObject (e.g. for header information)
    // JSONObject header = new JSONObject();
    // header.put("deviceType", "Android"); // Device type
    // header.put("deviceVersion", "1.5"); // Device OS version
    // header.put("language", "sv-se"); // Language of the Android client
    // jsonObjSend.put("header", header);
    //
    // // Add hardcoded inline attachment
    // JSONObject attachment = new JSONObject();
    // JSONObject doc = new JSONObject();
    // // doc.put("content_type", "text/plain");
    // doc.put("content_type", "image/jpeg");
    // // Base64 encoded image of red spot
    // String imageData =
    // "iVBORw0KGgoAAAANSUhEUgAAAAoAAAAKCAYAAACNMs+9AAAABGdBTUEAALGP"
    // + "C/xhBQAAAAlwSFlzAAALEwAACxMBAJqcGAAAAAd0SU1FB9YGARc5KB0XV+IA"
    // + "AAAddEVYdENvbW1lbnQAQ3JlYXRlZCB3aXRoIFRoZSBHSU1Q72QlbgAAAF1J"
    // + "REFUGNO9zL0NglAAxPEfdLTs4BZM4DIO4C7OwQg2JoQ9LE1exdlYvBBeZ7jq"
    // + "ch9//q1uH4TLzw4d6+ErXMMcXuHWxId3KOETnnXXV6MJpcq2MLaI97CER3N0"
    // + "vr4MkhoXe0rZigAAAABJRU5ErkJggg==";
    // // Base64 encoded text
    // // String textData = "VGhpcyBpcyBhIGJhc2U2NCBlbmNvZGVkIHRleHQ=";
    // doc.put("data", imageData);
    // attachment.put("sig.jpeg", doc);
    // jsonObjSend.put("_attachments", attachment);
    //
    // // Output the JSON object we're sending to logcat:
    // Log.i(TAG, jsonObjSend.toString(2));
    //
    // } catch (JSONException e) {
    // e.printStackTrace();
    // }
    // return jsonObjSend;
    // }

}
