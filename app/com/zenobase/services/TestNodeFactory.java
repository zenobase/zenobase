package com.zenobase.services;

import java.io.File;

import com.google.common.io.Files;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

public class TestNodeFactory extends NodeFactorySupport {

	private final File path;

	public TestNodeFactory() {
		this.path = Files.createTempDir();
		this.path.deleteOnExit();
	}

	public TestNodeFactory(File path) {
		this.path = path;
	}

	@Override
	public Node createNode(String clusterName) {
		Logger.info("Starting test node...");
		ImmutableSettings.Builder settings = createDefaultSettings()
			.put("path.home", path.getAbsolutePath())
			.put("gateway.type", "none")
			.put("index.store.type", "memory");
		return NodeBuilder.nodeBuilder().clusterName(clusterName).client(false).local(true).settings(settings.build()).node();
	}
}
