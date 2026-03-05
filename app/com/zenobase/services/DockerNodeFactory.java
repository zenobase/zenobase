package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

public class DockerNodeFactory extends NodeFactorySupport {

	private final String host;
	private final String publishHost;

	@Inject
	public DockerNodeFactory(@Named("es.host") String host, @Named("es.publish_host") String publishHost) {
		this.host = host;
		this.publishHost = publishHost;
	}

	@Override
	public Node createNode(String clusterName) {
		Logger.info("Starting node in cluster {} connecting to {}...", clusterName, host);
		ImmutableSettings.Builder settings = createDefaultSettings()
			.put("discovery.zen.ping.unicast.hosts", host)
			.put("discovery.zen.ping.multicast.enabled", false);
		if (!publishHost.isEmpty()) {
			settings.put("network.publish_host", publishHost);
		}
		return NodeBuilder.nodeBuilder().clusterName(clusterName).client(true).local(false).settings(settings.build()).node();
	}
}
