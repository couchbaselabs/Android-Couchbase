package com.couchbase.android;

import java.net.MalformedURLException;
import java.net.URL;
import java.text.Collator;

import android.os.Handler;
import android.os.Message;
import android.util.Log;

import com.ericsson.otp.erlang.OtpErlangDecodeException;
import com.ericsson.otp.erlang.OtpErlangInt;
import com.ericsson.otp.erlang.OtpErlangObject;
import com.ericsson.otp.erlang.OtpErlangRef;
import com.ericsson.otp.erlang.OtpErlangTuple;
import com.ericsson.otp.erlang.OtpInputStream;
import com.ericsson.otp.erlang.OtpOutputStream;

public class ErlangThread {
	public static native void start_erlang(String bindir, String sopath, String[] args);
	private static native void send_bin(byte[] binary, long caller);
	
	private static Handler mHandler;
	
	private static void erl_message(String name, byte[] message, long caller) {
		//Process messages from jninif:cast and jninif:call.
		//Call replies must be sent BEFORE this function returns.
		if(mHandler == null) { Log.v("JNINIF", "No handler set."); return; }
		try {
			if(name.equals("started"))
			{
					try {
						String url = new String(new OtpInputStream(message).read_binary());
						Message.obtain(mHandler, CouchbaseService.COUCHBASE_STARTED, new URL(url)).sendToTarget();
					} catch (MalformedURLException e) {
						Log.e("JNINIF", "URL Exception", e);
					} 
			} else if (name.startsWith("log")) {
				String log = new String(new OtpInputStream(message).read_binary());
				if(name.endsWith("info")) Log.i("CouchDB", log);
				else if (name.endsWith("debug")) Log.d("CouchDB", log);
				else if (name.endsWith("error")) Log.e("CouchDB", log);
			} else if(name.startsWith("collate")) {
				OtpInputStream i = new OtpInputStream(message);
				i.read_tuple_head();
				OtpErlangRef tag = i.read_ref();
				i.read_tuple_head();
				String a = new String(i.read_binary());
				String b = new String(i.read_binary());
				Collator c = Collator.getInstance();
				if(name.equals("collate_nocase")) c.setStrength(Collator.PRIMARY);
				int res = c.compare(a, b);
				reply(tag, new OtpErlangInt(res), caller);
			}
		}
		catch (OtpErlangDecodeException e) {
			Log.e("JNINIF", "Erlang Decode Exception", e);
		}
	}
	
	private static void reply(OtpErlangRef tag, OtpErlangObject reply, long caller)
	{
		send_term(new OtpErlangTuple(new OtpErlangObject[]{tag, reply}), caller);
	}
	
	private static void send_term(OtpErlangObject obj, long caller) {
		OtpOutputStream out = new OtpOutputStream();
		out.write(131);
		obj.encode(out);
		send_bin(out.toByteArray(), caller);
	}
	
	public static void setHandler(Handler h) {
		mHandler = h;
	}
	
	static {
		System.loadLibrary("beam");
		System.loadLibrary("com_couchbase_android_ErlangThread");
	}
}
