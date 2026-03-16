package com.zenobase.services;

import java.net.URI;

import javax.inject.Inject;
import javax.inject.Named;

import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import play.Logger;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;

public class OpenSearchClientFactory implements ClientFactory {

	private final String host;
	private final String region;

	@Inject
	public OpenSearchClientFactory(@Named("opensearch.host") String host, @Named("opensearch.snapshot.region") String region) {
		this.host = host;
		this.region = region;
	}

	@Override
	public OpenSearchClient createClient() {
		Logger.info("Connecting to {}...", host);
		URI uri = URI.create(host);
		if ("https".equals(uri.getScheme())) {
			SdkHttpClient httpClient = AwsCrtHttpClient.builder().build();
			AwsSdk2Transport transport = new AwsSdk2Transport(
				httpClient,
				uri.getHost(),
				"es",
				Region.of(region),
				AwsSdk2TransportOptions.builder().build()
			);
			return new OpenSearchClient(transport);
		}
		HttpHost httpHost = HttpHost.create(uri);
		ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder
			.builder(httpHost)
			.setMapper(new JacksonJsonpMapper())
			.build();
		return new OpenSearchClient(transport);
	}
}
