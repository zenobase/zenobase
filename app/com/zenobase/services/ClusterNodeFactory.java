package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

public class ClusterNodeFactory extends NodeFactorySupport {

	private final String accessKey;
	private final String secretKey;
	private final String region;

	@Inject
	public ClusterNodeFactory(@Named("aws.access_key") String accessKey, @Named("aws.secret_key") String secretKey, String region) {
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
	}

	@Override
	public Node createNode(String clusterName) {
		Logger.info("Starting node in cluster " + clusterName + "...");
		ImmutableSettings.Builder settings = createDefaultSettings()
			.put("cloud.aws.access_key", accessKey)
			.put("cloud.aws.secret_key", secretKey)
			.put("cloud.aws.region", region)
			.put("discovery.type", "ec2");
		return NodeBuilder.nodeBuilder().clusterName(clusterName).client(true).local(false).settings(settings.build()).node();
	}
}
