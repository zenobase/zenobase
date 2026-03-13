package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import play.Logger;

public class OpenSearchClientFactory implements ClientFactory {

	private final String host;

	@Inject
	public OpenSearchClientFactory(@Named("es.host") String host) {
		this.host = host;
	}

	@Override
	public OpenSearchClient createClient() {
		Logger.info("Connecting to {}...", host);
		HttpHost httpHost = HttpHost.create(java.net.URI.create(host));
		ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder
			.builder(httpHost)
			.setMapper(new JacksonJsonpMapper())
			.build();
		return new OpenSearchClient(transport);
	}
}
