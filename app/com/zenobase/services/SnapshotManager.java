package com.zenobase.services;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

public class SnapshotManager implements Closeable {

	private final Node node;
	private final Client client;
	private final String repositoryName;

	@Inject
	public SnapshotManager(NodeFactory nodeFactory, @Named("es.cluster") String clusterName) {
		node = nodeFactory.createNode(clusterName);
		client = node.client();
		this.repositoryName = clusterName;
		while (!new Cluster(client).isReady()) {
			Logger.warn("Waiting for cluster to recover...");
		}
	}

	public void snapshot() {
		client.admin().cluster().prepareCreateSnapshot(repositoryName, DateTime.now(DateTimeZone.UTC).toString());
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
