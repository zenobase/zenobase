package services;

import java.io.Closeable;

import org.elasticsearch.action.admin.cluster.health.ClusterHealthStatus;
import org.elasticsearch.client.Client;
import org.elasticsearch.common.settings.ImmutableSettings;
import org.elasticsearch.common.settings.Settings;
import org.elasticsearch.common.unit.TimeValue;
import org.elasticsearch.node.Node;
import org.elasticsearch.node.NodeBuilder;

import play.Logger;

public class NodeManager implements Closeable {

	private Node node;
	private Client client;

	public NodeManager() {
		Settings settings = ImmutableSettings.settingsBuilder()
			.put("index.mapper.dynamic", false)
			.put("index.cache.filter.type", "none")
			.put("action.auto_create_index", false).build();
		node = NodeBuilder.nodeBuilder().settings(settings).node();
		client = node.client();
		recover();
		Logger.info("Started node: " + getHealthStatus());
	}

	private void recover() {
		ClusterHealthStatus status = getHealthStatus();
		if (ClusterHealthStatus.RED.equals(status)) {
			status = client.admin().cluster().prepareHealth().setWaitForYellowStatus().setTimeout(new TimeValue(30000)).execute().actionGet().getStatus();
		}
	}

	private ClusterHealthStatus getHealthStatus() {
		return client.admin().cluster().prepareHealth().execute().actionGet().getStatus();
	}

	public IndexManager getIndex(String indexName) {
		return new IndexManager(indexName, client);
	}

	@Override
    public void close() {
		Logger.info("Closing node: " + getHealthStatus());
		client.close();
		node.close();
	}
}
