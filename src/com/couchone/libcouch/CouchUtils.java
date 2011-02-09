package com.couchone.libcouch;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.util.Random;

import android.app.Activity;
import android.content.res.AssetManager;
import android.os.Environment;
import android.webkit.HttpAuthHandler;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CouchUtils {
	
	/*
	 * Create a WebView with the sensible defaults, and load a url within it
	 */
	public static WebView launchUrl(Activity activity, String url, String user, String pass) {
		WebView webView = new WebView(activity);
		webView.setWebChromeClient(new WebChromeClient());
		webView.setWebViewClient(new CouchUtils.CustomWebViewClient());
		webView.getSettings().setJavaScriptEnabled(true);
		webView.getSettings().setDomStorageEnabled(true);
		webView.setScrollBarStyle(WebView.SCROLLBARS_OUTSIDE_OVERLAY);
		webView.setHttpAuthUsernamePassword("127.0.0.1", "administrator", user, pass);
		webView.requestFocusFromTouch();
		webView.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
		activity.setContentView(webView);
		webView.loadUrl(url);
		return webView;
	};

	
	public static String readAsset(AssetManager assets, String path) throws IOException {
		InputStream is = assets.open(path);
		int size = is.available();
		byte[] buffer = new byte[size];
		is.read(buffer);
		is.close();
		return new String(buffer);
	}

	public static boolean inArray(String[] haystack, String needle) {
		for (int i = 0; i < haystack.length; i++) {
			if (haystack[0].equals(needle)) {
				return true;
			}
		}
		return false;
	}
	
	public static String readOrGeneratePass(String user) {
		return readOrGeneratePass(user, generatePassword(8));
	}

	public static String readOrGeneratePass(String username, String pass) {
		String passFile =  Environment.getExternalStorageDirectory() + "/couch/" + username + ".passwd";
		File f = new File(passFile);
		if (!f.exists()) {
			writeFile(passFile, username + ":" + pass);
			return pass;
		} else {
			return readFile(passFile).split(":")[1];
		}
	}
	
	/*
	 * The custom webview client ensures that when users click links, it opens
	 * in the current webview and doesnt open the browser
	 */
	private static class CustomWebViewClient extends WebViewClient {
		@Override
		public boolean shouldOverrideUrlLoading(WebView view, String url) {
			view.loadUrl(url);
			return true;
		}

		@Override
		public void onReceivedHttpAuthRequest(WebView view,
				HttpAuthHandler handler, String host, String realm) {
			String[] up = view.getHttpAuthUsernamePassword(host, realm);
			handler.proceed(up[0], up[1]);
		}
	}

	public static String generatePassword(int length) {
		String charset = "!0123456789abcdefghijklmnopqrstuvwxyz";
		Random rand = new Random(System.currentTimeMillis());
		StringBuffer sb = new StringBuffer();
		for (int i = 0; i < length; i++) {
			int pos = rand.nextInt(charset.length());
			sb.append(charset.charAt(pos));
		}
		return sb.toString();
	}
	
	private static String readFile(String filePath) {
		String contents = "";
		try {
			File file = new File(filePath);
			BufferedReader reader = new BufferedReader(new FileReader(file));
			contents = reader.readLine();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		return contents;
	};

	private static void writeFile(String filePath, String data) {
		try {
			FileWriter writer = new FileWriter(filePath);
			writer.write(data);
			writer.close();
		} catch (IOException e) {
			e.printStackTrace();
		}
	}


}
