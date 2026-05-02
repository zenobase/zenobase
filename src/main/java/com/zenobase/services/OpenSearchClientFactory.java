package com.zenobase.services;

import com.zenobase.json.Nodes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.time.Duration;
import java.util.concurrent.TimeUnit;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.core5.http.HttpHost;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.opensearch.client.transport.httpclient5.ApacheHttpClient5TransportBuilder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;

public class OpenSearchClientFactory implements ClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchClientFactory.class);

	private final String host;
	private final String region;

	@Inject
	public OpenSearchClientFactory(@Named("opensearch.host") String host, @Named("aws.region") String region) {
		this.host = host;
		this.region = region;
	}

	@Override
	public OpenSearchClient createClient() {
		logger.info("Connecting to {}...", host);
		return createClient(host, region);
	}

	public static OpenSearchClient createClient(String host, String region) {
		var uri = URI.create(host);
		var mapper = new JacksonJsonpMapper(Nodes.MAPPER);
		if ("https".equals(uri.getScheme())) {
			var httpClient = AwsCrtHttpClient.builder().connectionTimeout(Duration.ofSeconds(30)).build();
			var transport = new AwsSdk2Transport(
				httpClient,
				uri.getHost(),
				"es",
				Region.of(region),
				AwsSdk2TransportOptions.builder().setMapper(mapper).build()
			);
			return new OpenSearchClient(transport);
		}
		var httpHost = HttpHost.create(uri);
		return new OpenSearchClient(
			ApacheHttpClient5TransportBuilder.builder(httpHost)
				.setMapper(mapper)
				.setHttpClientConfigCallback(builder ->
					builder
						.addRequestInterceptorFirst((request, entity, context) ->
							request.setHeader("Accept-Encoding", "gzip")
						)
						.setDefaultRequestConfig(
							RequestConfig.custom().setResponseTimeout(60, TimeUnit.SECONDS).build()
						)
				)
				.build()
		);
	}
}
