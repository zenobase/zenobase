package com.zenobase.services;

import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.rules.TemporaryFolder;

public abstract class ElasticSearchTestSupport {

	@Rule
	public TemporaryFolder folder = new TemporaryFolder();

	private Node node;
	private Client client;
	private final String cluster = "test";

	@Before
	public void init() {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("path.home", folder.getRoot().getAbsolutePath())
			.put("gateway.type", "none")
			.put("index.store.type", "memory").build();
		node = NodeBuilder.nodeBuilder().clusterName(cluster).local(true).settings(settings).node();
		client = node.client();
	}

	protected Client getClient() {
		return client;
	}

	@After
	public void close() {
		client.close();
		node.close();
	}
}
