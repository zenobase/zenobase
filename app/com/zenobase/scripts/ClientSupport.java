package com.zenobase.scripts;

import java.io.IOException;

import org.apache.http.client.HttpClient;
import org.apache.http.impl.client.DefaultHttpClient;

public abstract class ClientSupport {

	protected final HttpClient client = new DefaultHttpClient();
	protected final String host;
	protected final String callback;
	protected final String apiKey;
	protected final String token;

	protected ClientSupport() {
		host = System.getProperty("api.host", "https://zenobase.com");
		callback = System.getProperty("api.callback", host + "/test/");
		apiKey = System.getProperty("api.key");
		token = System.getProperty("api.token");
	}

	public void run() throws IOException {

		if (token == null) {
			System.err.format("%s/#/oauth/authorize?response_type=token&client_id=%s&redirect_uri=%s\n", host, apiKey, callback);
			System.exit(1);
		}

		doRun();
	}

	protected abstract void doRun() throws IOException;
}
