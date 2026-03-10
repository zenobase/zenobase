package com.zenobase.services;

import static org.mockito.Mockito.*;

import java.io.IOException;
import java.time.Duration;

import org.apache.http.HttpHost;
import org.junit.After;
import org.junit.Before;
import org.opensearch.action.admin.indices.delete.DeleteIndexRequest;
import org.opensearch.client.RestClient;
import org.opensearch.client.RestHighLevelClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

import com.zenobase.common.Callback;

public abstract class ElasticSearchTestSupport {

	private static final GenericContainer<?> container;
	static {
		container = new GenericContainer<>("opensearchproject/opensearch:2.19.4")
			.withEnv("discovery.type", "single-node")
			.withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
			.withEnv("plugins.security.disabled", "true")
			.withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
			.withEnv("compatibility.override_main_response_version", "true")
			.withExposedPorts(9200)
			.waitingFor(Wait.forHttp("/_cluster/health").forStatusCode(200)
				.withStartupTimeout(Duration.ofMinutes(2)));
		container.start();
	}

	private ClientFactory clientFactory;
	private IndexManager manager;

	@Before
	public void createManager() {
		String host = "http://" + container.getHost() + ":" + container.getMappedPort(9200);
		clientFactory = () -> new RestHighLevelClient(RestClient.builder(HttpHost.create(host)));
		try (RestHighLevelClient client = clientFactory.createClient()) {
			client.indices().delete(new DeleteIndexRequest("*"), TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			// OK if no indices exist
		}
		manager = new IndexManager(clientFactory);
	}

	protected ClientFactory getClientFactory() {
		return clientFactory;
	}

	protected IndexManager getManager() {
		return manager;
	}

	@After
	public void closeManager() {
		manager.close();
	}

	protected static <T> void verifyInteractions(Callback<T> callback, Iterable<T> expected) {
		for (T t : expected) {
			verify(callback).call(t);
		}
		verifyNoMoreInteractions(callback);
	}
}
