package com.zenobase.scripts;

import java.io.IOException;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.core5.http.ClassicHttpResponse;
import org.apache.hc.core5.http.io.entity.EntityUtils;

import com.zenobase.json.Nodes;

public abstract class ClientSupport {

	protected final CloseableHttpClient client = HttpClients.createDefault();
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

	protected static ObjectNode readObject(ClassicHttpResponse response) throws IOException {
		return Nodes.readObject(EntityUtils.toByteArray(response.getEntity()));
	}

	public void run() throws IOException {
		if (token.isBlank()) {
			System.err.format(
					"%s/#/oauth/authorize?response_type=token&client_id=%s&redirect_uri=%s\n", host, apiKey, callback);
			System.exit(1);
		}

		doRun();
	}

	protected abstract void doRun() throws IOException;
}
