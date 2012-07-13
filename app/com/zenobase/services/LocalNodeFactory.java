package com.zenobase.services;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

public class LocalNodeFactory extends NodeFactorySupport {

	@Override
	public Node createNode(String clusterName) {
		Logger.info("Starting local node...");
		ImmutableSettings.Builder settings = createDefaultSettings();
		return NodeBuilder.nodeBuilder().clusterName(clusterName).client(false).local(true).settings(settings.build()).node();
	}
}
