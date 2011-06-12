package com.couchbase.libcouch;

/*
 * AndCouch is a very simple http wrapper library for CouchDB with minimal
 * dependencies
 *
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.JSONException;
import org.json.JSONObject;

public class AndCouch {

	public String[][] headers;
	public JSONObject json;
	public String result;
	public int status;

	public AndCouch(String[][] headers, JSONObject json, String result,
			int status) {
		this.headers = headers;
		this.json = json;
		this.result = result;
		this.status = status;
	}

	public static AndCouch post(String url, String data) throws JSONException {
		return post(url, data, new String[][]{});
	}

	public static AndCouch post(String url, String data, String[][] headers)
			throws JSONException {
		return AndCouch.httpRequest("POST", url, data, headers);
	}

	public static AndCouch put(String url, String data) throws JSONException {
		return put(url, data, new String[][]{});
	}

	public static AndCouch put(String url, String data, String[][] headers)
			throws JSONException {
		return AndCouch.httpRequest("PUT", url, data, headers);
	}

	public static AndCouch get(String url) throws JSONException {
		return get(url, new String[][] {});
	}

	public static AndCouch get(String url, String[][] headers)
			throws JSONException {
		return AndCouch.httpRequest("GET", url, null, headers);
	}

	public static AndCouch httpRequest(String method, String url,
			String data, String[][] headers) throws JSONException {

		StringBuffer sb = new StringBuffer();
		int statusCode = 0;

		try {

			HttpURLConnection c = (HttpURLConnection) new URL(url).openConnection();
			String charEncoding = "iso-8859-1";

			c.setDoOutput(true);
			c.setUseCaches(false);

			c.setRequestMethod(method);
			c.setRequestProperty("Content-type", "application/json; charset="
					+ "UTF-8");

			for (String[] tmp : headers) {
				c.setRequestProperty(tmp[0], tmp[1]);
			}

			if (method != "GET" && data != null) {
				c.setDoInput(true);
				c.setRequestProperty("Content-Length",
						Integer.toString(data.length()));
				c.getOutputStream().write(data.getBytes(charEncoding));
			}

			c.connect();

			statusCode = c.getResponseCode();

			// TODO: Nasty
			try {
				BufferedReader rd = new BufferedReader(new InputStreamReader(
						c.getInputStream()));
				String line;
				while ((line = rd.readLine()) != null) {
					sb.append(line);
				}
				rd.close();
			} catch (FileNotFoundException e) {

			} catch (NullPointerException e) {

			}

			finally {
				c.disconnect();
			}

		} catch (IOException e) {
			e.printStackTrace();
		}

		JSONObject json= sb.toString().length() == 0
			? new JSONObject()
			: new JSONObject(sb.toString());

		return new AndCouch(headers, json, sb.toString(), statusCode);
	};

	@Override
	public String toString() {
		return "HTTPResult -> status: " + Integer.toString(status);
	}
}
