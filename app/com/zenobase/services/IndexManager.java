package com.zenobase.services;

import java.io.Closeable;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import play.Logger;

public class IndexManager implements Closeable {

	private Node node;
	private Client client;

	public IndexManager() {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("index.mapper.dynamic", false)
			.put("index.cache.filter.type", "none")
			.put("action.auto_create_index", false).build();
		node = NodeBuilder.nodeBuilder().settings(settings).node();
		client = node.client();
		recover();
		Logger.info("Started node: " + getHealthStatus());
	}

	private void recover() {
		ClusterHealthStatus status = getHealthStatus();
		if (ClusterHealthStatus.RED.equals(status)) {
			Logger.info("Recovering...");
			status = client.admin().cluster().prepareHealth().setWaitForYellowStatus().setTimeout(new TimeValue(30000)).execute().actionGet().getStatus();
		}
	}

	private ClusterHealthStatus getHealthStatus() {
		return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
	}

	public Index getIndex(String indexName) {
		return new Index(indexName, client);
	}

	public void flush() {
		client.admin().indices().prepareFlush("_all").execute().actionGet();
	}

	@Override
	public void close() {
		Logger.info("Closing node: " + getHealthStatus());
		flush();
		client.close();
		node.close();
	}
}
