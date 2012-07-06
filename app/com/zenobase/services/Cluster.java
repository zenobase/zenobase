package com.zenobase.services;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.unit.TimeValue;

public class Cluster {

	private final Client client;

	public Cluster(Client client) {
		this.client = client;
	}

	public ClusterHealthStatus getHealthStatus() {
		return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
	}

	public boolean isReady() {
		return client.admin().cluster().prepareHealth().setWaitForYellowStatus().setTimeout(new TimeValue(30000))
			.execute().actionGet().getStatus() != ClusterHealthStatus.RED;
	}
}
