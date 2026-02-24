package com.zenobase.services;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;
import play.Logger;

public class DockerNodeFactory extends NodeFactorySupport {

	private final String host;
	private final String replayHost;
	private final String replayCluster;
	private final String accessKey;
	private final String secretKey;
	private final String region;
	private final String bucket;

	@Inject
	public DockerNodeFactory(@Named("es.host") String host, @Named("es.replay.host") String replayHost, @Named("es.replay.cluster") String replayCluster, @Named("aws.access_key") String accessKey, @Named("aws.secret_key") String secretKey, @Named("aws.region") String region, @Named("aws.bucket") String bucket) {
		this.host = host;
		this.replayHost = replayHost;
		this.replayCluster = replayCluster;
		this.accessKey = accessKey;
		this.secretKey = secretKey;
		this.region = region;
		this.bucket = bucket;
	}

	@Override
	public Node createNode(String clusterName) {
		String unicastHost = host;
		if (!replayHost.isEmpty() && clusterName.equals(replayCluster)) {
			unicastHost = replayHost;
			Logger.info("Starting replay node in cluster {} connecting to {}...", clusterName, unicastHost);
		} else {
			Logger.info("Starting node in cluster {} connecting to {}...", clusterName, unicastHost);
		}
		ImmutableSettings.Builder settings = createDefaultSettings()
			.put("discovery.zen.ping.unicast.hosts", unicastHost)
			.put("discovery.zen.ping.multicast.enabled", false);
		if (!accessKey.isEmpty()) {
			settings.put("cloud.aws.access_key", accessKey)
				.put("cloud.aws.secret_key", secretKey)
				.put("cloud.aws.region", region);
		}
		Node node = NodeBuilder.nodeBuilder().clusterName(clusterName).client(true).local(false).settings(settings.build()).node();
		if (!accessKey.isEmpty() && !bucket.isEmpty()) {
			registerSnapshotRepository(node, clusterName.toLowerCase());
		}
		return node;
	}

	private void registerSnapshotRepository(Node node, String repositoryName) {
		node.client().admin().cluster().preparePutRepository(repositoryName).setType("s3")
			.setSettings(ImmutableSettings.builder().put("bucket", bucket).put("base_path", repositoryName))
			.get();
	}
}
