package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import play.Logger;

public class RestClientFactory implements ClientFactory {

	private final String host;

	@Inject
	public RestClientFactory(@Named("es.host") String host) {
		this.host = host;
	}

	@Override
	public OpenSearchClient createClient() {
		Logger.info("Connecting to {}...", host);
		RestClient restClient = RestClient.builder(HttpHost.create(java.net.URI.create(host))).build();
		RestClientTransport transport = new RestClientTransport(restClient, new JacksonJsonpMapper());
		return new OpenSearchClient(transport);
	}
}
