package com.zenobase.scripts;

import java.io.IOException;
import java.security.KeyManagementException;
import java.security.NoSuchAlgorithmException;

import javax.net.ssl.SSLContext;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.conn.ssl.SSLContexts;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.util.EntityUtils;

import com.zenobase.json.Nodes;

public abstract class ClientSupport {

	protected final HttpClient client = buildClient();
	protected final String host;
	protected final String callback;
	protected final String apiKey;
	protected final String token;

	protected ClientSupport() {
		host = System.getProperty("api.host", "https://api.zenobase.com");
		callback = System.getProperty("api.callback", host + "/test/");
		apiKey = System.getProperty("api.key");
		token = System.getProperty("api.token");
	}

	private HttpClient buildClient() {
		try {
			SSLContext context = SSLContexts.custom().useTLS().build();
			SSLConnectionSocketFactory f = new SSLConnectionSocketFactory(context,
				new String[] { "TLSv1.1" }, null,
				SSLConnectionSocketFactory.BROWSER_COMPATIBLE_HOSTNAME_VERIFIER);
			return HttpClients.custom().setSSLSocketFactory(f).build();
		} catch (KeyManagementException | NoSuchAlgorithmException e) {
			throw new RuntimeException(e);
		}
	}

	protected static ObjectNode readObject(HttpResponse response) throws IOException {
		return Nodes.readObject(EntityUtils.toByteArray(response.getEntity()));
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
