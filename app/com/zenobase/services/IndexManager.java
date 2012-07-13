package com.zenobase.services;

import java.io.Closeable;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.client.Client;
import org.elasticsearch.node.Node;
import play.Logger;

public class IndexManager implements Closeable {

	private Node node;
	private Client client;

	@Inject
	public IndexManager(NodeFactory nodeFactory, @Named("es.cluster") String clusterName) {
		node = nodeFactory.createNode(clusterName);
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
