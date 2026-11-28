package com.zenobase.services;

import com.google.common.base.Preconditions;
import com.zenobase.json.Nodes;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.time.Duration;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.aws.AwsSdk2Transport;
import org.opensearch.client.transport.aws.AwsSdk2TransportOptions;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.http.ExecutableHttpRequest;
import software.amazon.awssdk.http.HttpExecuteRequest;
import software.amazon.awssdk.http.SdkHttpClient;
import software.amazon.awssdk.http.crt.AwsCrtHttpClient;
import software.amazon.awssdk.regions.Region;

public class OpenSearchClientFactory implements ClientFactory {

	private static final Logger logger = LoggerFactory.getLogger(OpenSearchClientFactory.class);

	private final String host;
	private final String region;

	@Inject
	public OpenSearchClientFactory(@Named("opensearch.host") String host, @Named("aws.region") String region) {
		Preconditions.checkState(!host.isBlank(), "opensearch.host is not configured");
		this.host = host;
		this.region = region;
		Preconditions.checkState(
			!host.startsWith("https") || !region.isBlank(),
			"aws.region is required when opensearch.host is https"
		);
	}

	@Override
	public OpenSearchClient createClient() {
		logger.info("Connecting to {}...", host);
		return createClient(host, region);
	}

	public static OpenSearchClient createClient(String host, String region) {
		var uri = URI.create(host);
		boolean https = "https".equals(uri.getScheme());
		Preconditions.checkArgument(
			!https || (region != null && !region.isBlank()),
			"A region (aws.region or AWS_REGION) is required for %s",
			host
		);
		SdkHttpClient httpClient = AwsCrtHttpClient.builder().connectionTimeout(Duration.ofSeconds(30)).build();
		var options = AwsSdk2TransportOptions.builder().setMapper(new JacksonJsonpMapper(Nodes.MAPPER));
		if (https) {
			return createClient(httpClient, uri.getHost(), Region.of(region), options.build());
		}
		// Local cluster without the security plugin: it ignores the SigV4 header, so any credentials and region will do.
		options.setCredentials(StaticCredentialsProvider.create(AwsBasicCredentials.create("local", "local")));
		return createClient(new PlainHttpClient(httpClient), uri.getAuthority(), Region.US_EAST_1, options.build());
	}

	private static OpenSearchClient createClient(
		SdkHttpClient httpClient,
		String host,
		Region region,
		AwsSdk2TransportOptions options
	) {
		return new OpenSearchClient(
			new AwsSdk2Transport(httpClient, host, "es", region, options) {
				// AwsSdk2Transport doesn't close the HTTP client it's given.
				@Override
				public void close() {
					httpClient.close();
				}
			}
		);
	}

	/** {@link AwsSdk2Transport} always builds {@code https://} URIs; this downgrades them for a local cluster. */
	private record PlainHttpClient(SdkHttpClient delegate) implements SdkHttpClient {
		@Override
		public ExecutableHttpRequest prepareRequest(HttpExecuteRequest request) {
			return delegate.prepareRequest(
				HttpExecuteRequest.builder()
					.request(request.httpRequest().toBuilder().protocol("http").build())
					.contentStreamProvider(request.contentStreamProvider().orElse(null))
					.metricCollector(request.metricCollector().orElse(null))
					.build()
			);
		}

		@Override
		public void close() {
			delegate.close();
		}
	}
}
