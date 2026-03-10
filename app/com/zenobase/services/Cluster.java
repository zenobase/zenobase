package com.zenobase.services;

import java.io.IOException;

import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch.cluster.HealthRequest;
import org.opensearch.client.opensearch.cluster.HealthResponse;
import org.opensearch.client.opensearch._types.HealthStatus;

public class Cluster {

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
}
