package com.zenobase.services;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;

import org.apache.hc.core5.http.HttpHost;
import org.junit.After;
import org.junit.Before;
import org.opensearch.client.RestClient;
import org.opensearch.client.json.jackson.JacksonJsonpMapper;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.transport.rest_client.RestClientTransport;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.zenobase.common.Callback;

public abstract class ElasticSearchTestSupport {

	private static final GenericContainer<?> container;
	private static final OpenSearchClient sharedClient;
	static {
		container = new GenericContainer<>("opensearchproject/opensearch:3.1.0")
			.withEnv("discovery.type", "single-node")
			.withEnv("plugins.security.disabled", "true")
			.withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
			.withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
			.withExposedPorts(9200)
			.waitingFor(Wait.forHttp("/_cluster/health").forStatusCode(200)
				.withStartupTimeout(Duration.ofMinutes(2)));
		container.start();
		String host = "http://" + container.getHost() + ":" + container.getMappedPort(9200);
		RestClient restClient = RestClient.builder(HttpHost.create(java.net.URI.create(host))).build();
		sharedClient = new OpenSearchClient(new RestClientTransport(restClient, new JacksonJsonpMapper()));
	}

	private IndexManager manager;

	@Before
	public void createManager() {
		try {
			sharedClient.indices().delete(d -> d.index("*"));
		} catch (IOException e) {
			// OK if no indices exist
		}
		manager = new IndexManager(() -> sharedClient);
	}

	protected ClientFactory getClientFactory() {
		return () -> sharedClient;
	}

	protected IndexManager getManager() {
		return manager;
	}

	@After
	public void closeManager() {
		// Shared client is reused across all tests; don't close it
	}

	protected static <T> void verifyInteractions(Callback<T> callback, Iterable<T> expected) {
		for (T t : expected) {
			verify(callback).call(t);
		}
		verifyNoMoreInteractions(callback);
	}
}
