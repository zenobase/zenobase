package com.zenobase.services;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Named;

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

	@Inject
	public IndexManager(@Named("es.cluster") String clusterName, @Named("es.clientOnly") boolean clientOnly) {
		this(clusterName, clientOnly, false, ImmutableSettings.Builder.EMPTY_SETTINGS);
	}

	public IndexManager(String clusterName, boolean clientOnly, boolean local, Settings defaultSettings) {
		Logger.info("Starting node in cluster " + clusterName + "...");
		ImmutableSettings.Builder settings = ImmutableSettings.settingsBuilder()
			.put(defaultSettings)
			.put("index.mapper.dynamic", false)
			.put("index.cache.filter.type", "none")
			.put("index.cache.field.type", "soft")
			.put("action.auto_create_index", false);
		if (clientOnly) {
			settings.put("cloud.aws.access_key", "AKIAI7MHM3G2FTHQJJ3A");
			settings.put("cloud.aws.secret_key", "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");
			settings.put("cloud.aws.region", "us-east-1");
			settings.put("discovery.type", "ec2");
		}
		node = NodeBuilder.nodeBuilder().clusterName(clusterName).client(clientOnly).local(local).settings(settings.build()).node();
		client = node.client();
		recover();
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

	@Override
	public void close() {
		Logger.info("Closing node: " + getHealthStatus());
		flush();
		client.close();
		node.close();
	}

	private void flush() {
		client.admin().indices().prepareFlush("_all").execute().actionGet();
	}
}
