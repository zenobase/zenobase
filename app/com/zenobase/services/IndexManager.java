package com.zenobase.services;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
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
		while (!new Cluster(client).isReady()) {
			Logger.warn("Waiting for cluster to recover...");
		}
	}

	public Index getIndex(String indexName) {
		return new Index(indexName, client);
	}

	public Cluster getCluster() {
		return new Cluster(client);
	}

	@Override
	public void close() {
		Logger.info("Closing node...");
		flush();
		client.close();
		node.close();
	}

	private void flush() {
		client.admin().indices().prepareFlush("_all").execute().actionGet();
	}
}
