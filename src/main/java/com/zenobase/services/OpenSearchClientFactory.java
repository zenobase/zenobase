package com.zenobase.services;

import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;

import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5Transport;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;

public class OpenSearchClientFactory implements ClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchClientFactory.class);

	private final String host;
	private final String region;

	public OpenSearchClientFactory(String host, String region) {
		this.host = host;
		this.region = region;
	}

	@Override
	public OpenSearchClient createClient() {
		logger.info("Connecting to {}...", host);
		URI uri = URI.create(host);
		if ("https".equals(uri.getScheme())) {
			SdkHttpClient httpClient = AwsCrtHttpClient.builder()
					.connectionTimeout(Duration.ofSeconds(30))
					.build();
			AwsSdk2Transport transport = new AwsSdk2Transport(
					httpClient,
					uri.getHost(),
					"es",
					Region.of(region),
					AwsSdk2TransportOptions.builder().build());
			return new OpenSearchClient(transport);
		}
		HttpHost httpHost = HttpHost.create(uri);
		ApacheHttpClient5Transport transport = ApacheHttpClient5TransportBuilder.builder(httpHost)
				.setMapper(new JacksonJsonpMapper())
				.build();
		return new OpenSearchClient(transport);
	}

	public static OpenSearchClient createHttpClient(String host) {
		HttpHost httpHost = HttpHost.create(URI.create(host));
		return new OpenSearchClient(ApacheHttpClient5TransportBuilder.builder(httpHost)
				.setMapper(new JacksonJsonpMapper())
				.setHttpClientConfigCallback(builder -> builder.addRequestInterceptorFirst(
								(request, entity, context) -> request.setHeader("Accept-Encoding", "gzip"))
						.setDefaultRequestConfig(RequestConfig.custom()
								.setResponseTimeout(60, TimeUnit.SECONDS)
								.build()))
				.build());
	}
}
