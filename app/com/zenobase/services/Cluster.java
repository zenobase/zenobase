package com.zenobase.services;

import java.io.IOException;

import org.opensearch.action.admin.cluster.health.ClusterHealthRequest;
import org.opensearch.action.admin.cluster.health.ClusterHealthResponse;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.cluster.health.ClusterHealthStatus;
import org.opensearch.common.unit.TimeValue;

public class Cluster {

	private final RestHighLevelClient client;

	public Cluster(RestHighLevelClient client) {
		this.client = client;
	}

	public ClusterHealthResponse getHealth() {
		try {
			return client.cluster().health(new ClusterHealthRequest(), TypeInjectingInterceptor.OPTIONS);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public boolean isReady() {
		try {
			ClusterHealthRequest request = new ClusterHealthRequest()
				.waitForYellowStatus()
				.timeout(new TimeValue(30000));
			return client.cluster().health(request, TypeInjectingInterceptor.OPTIONS).getStatus() != ClusterHealthStatus.RED;
		} catch (IOException e) {
			return false;
		}
	}
}
