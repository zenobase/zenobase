package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.http.HttpHost;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import play.Logger;

public class RestClientFactory implements ClientFactory {

	private final String host;

	@Inject
	public RestClientFactory(@Named("es.host") String host) {
		this.host = host;
	}

	@Override
	public RestHighLevelClient createClient() {
		Logger.info("Connecting to {}...", host);
		return new RestHighLevelClient(RestClient.builder(HttpHost.create(host)));
	}
}
