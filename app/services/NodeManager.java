package services;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import play.Logger;

public class NodeManager {

	private Node node;
	private Client client;

	public NodeManager() {
		Logger.info("Starting node...");
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("index.mapper.dynamic", false)
			.put("index.cache.filter.type", "none")
			.put("action.auto_create_index", false).build();
		node = NodeBuilder.nodeBuilder().settings(settings).node();
		client = node.client();
		recover();
	}

	private void recover() {
		ClusterHealthStatus status = getHealthStatus();
		Logger.info("Status: %s", status);
		if (ClusterHealthStatus.RED.equals(status)) {
			Logger.warn("Recovering: %s", status);
			status = client.admin().cluster().prepareHealth().setWaitForYellowStatus().setTimeout(new TimeValue(30000)).execute().actionGet().getStatus();
			Logger.info("Recovered: %s", status);
		}
	}

	private ClusterHealthStatus getHealthStatus() {
		return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
	}

	public IndexManager getIndex(String indexName) {
		return new IndexManager(indexName, client);
	}

    public void close() {
		client.close();
		node.close();
	}
}
