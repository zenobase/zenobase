package com.zenobase.services;

import java.io.IOException;

import jakarta.json.Json;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch._types.HealthStatus;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Cluster {

	private static final Logger logger = LoggerFactory.getLogger(Cluster.class);

	private final OpenSearchClient client;

	public Cluster(OpenSearchClient client) {
		this.client = client;
	}

	public HealthResponse getHealth() {
		try {
			return client.cluster().health(h -> h);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isReady() {
		try {
			HealthResponse response = client.cluster().health(h -> h
				.waitForStatus(HealthStatus.Yellow)
				.timeout(t -> t.time("30s"))
			);
			return response.status() != HealthStatus.Red;
		} catch (IOException e) {
			return false;
		}
	}

	public void disableAutoCreateIndex() {
		try {
			client.generic().execute(
				Requests.builder()
					.endpoint("/_cluster/settings")
					.method("PUT")
					.json(Json.createObjectBuilder()
						.add("persistent", Json.createObjectBuilder()
							.add("action.auto_create_index", "false")))
					.build()
			).close();
			logger.info("Disabled auto-creation of indices");
		} catch (IOException e) {
			throw new RuntimeException("Failed to disable auto-creation of indices", e);
		}
	}
}
