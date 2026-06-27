package com.zenobase.repositories;

import static org.mockito.Mockito.*;

import com.zenobase.common.Callback;
import com.zenobase.services.ClientFactory;
import com.zenobase.services.OpenSearchClientFactory;
import com.zenobase.testing.Integration;
import java.io.IOException;
import java.time.Duration;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.wait.strategy.Wait;

@Integration
public abstract class OpenSearchTestSupport {

	private static final GenericContainer<?> container;
	private static final OpenSearchClient sharedClient;

	static {
		container = new GenericContainer<>("opensearchproject/opensearch:3.5.0")
			.withEnv("discovery.type", "single-node")
			.withEnv("plugins.security.disabled", "true")
			.withEnv("DISABLE_INSTALL_DEMO_CONFIG", "true")
			.withEnv("OPENSEARCH_JAVA_OPTS", "-Xms512m -Xmx512m")
			.withEnv("path.repo", "/tmp/snapshots")
			.withExposedPorts(9200)
			.waitingFor(Wait.forHttp("/_cluster/health").forStatusCode(200).withStartupTimeout(Duration.ofMinutes(2)));
		container.start();
		String host = "http://" + container.getHost() + ":" + container.getMappedPort(9200);
		sharedClient = OpenSearchClientFactory.createClient(host, "us-east-1");
	}

	private IndexManager manager;

	@BeforeEach
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

	@AfterEach
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
