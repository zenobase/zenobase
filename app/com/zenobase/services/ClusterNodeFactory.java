package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

public class ClusterNodeFactory extends NodeFactorySupport {

	private final String bucket;

	@Inject
	public ClusterNodeFactory(@Named("es.snapshot.bucket") String bucket) {
		this.bucket = bucket;
	}

	@Override
	public Node createNode(String clusterName) {
		Logger.info("Starting node in cluster {}...", clusterName);
		ImmutableSettings.Builder settings = createDefaultSettings()
			.put("discovery.type", "ec2")
			.put("discovery.ec2.ping_timeout", "15s");
		Node node = NodeBuilder.nodeBuilder().clusterName(clusterName).client(true).local(false).settings(settings.build()).node();
		registerSnapshotRepository(node, clusterName.toLowerCase());
		return node;
	}

	private void registerSnapshotRepository(Node node, String repositoryName) {
		node.client().admin().cluster().preparePutRepository(repositoryName).setType("s3")
			.setSettings(ImmutableSettings.builder().put("bucket", bucket).put("base_path", repositoryName))
			.get();
	}
}
