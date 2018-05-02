package com.zenobase.services;

import java.io.Closeable;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesRequestBuilder;
import org.elasticsearch.action.admin.indices.alias.IndicesAliasesResponse;
import org.elasticsearch.client.Client;
import org.elasticsearch.cluster.metadata.AliasAction;
import org.elasticsearch.index.query.BoolFilterBuilder;
import org.elasticsearch.node.Node;
import play.Logger;

import com.zenobase.models.Alias;
import com.zenobase.models.Event;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.SearchBuilderSupport;

public class IndexManager implements Closeable {

	private final String clusterName;
	private Node node;
	private Client client;

	@Inject
	public IndexManager(NodeFactory nodeFactory, @Named("es.cluster") String clusterName) {
		this.clusterName = clusterName;
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

	public SnapshotManager getSnapshotManager() {
		return new SnapshotManager(client, clusterName.toLowerCase());
	}

	public void createAlias(String indexName, String aliasName, List<Alias> targets) {
		IndicesAliasesRequestBuilder request = client.admin().indices().prepareAliases();
		buildAlias(indexName, aliasName, targets, request);
		IndicesAliasesResponse response = request.get();
		Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of alias creation: %s", aliasName);
	}

	public void updateAlias(String indexName, String aliasName, List<Alias> targets) {
		Preconditions.checkArgument(!targets.isEmpty(), "Can't remove all aliases from %s", aliasName);
		IndicesAliasesRequestBuilder request = client.admin().indices().prepareAliases();
		request.removeAlias(indexName, aliasName);
		buildAlias(indexName, aliasName, targets, request);
		IndicesAliasesResponse response = request.get();
		Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of alias update: %s", aliasName);
	}

	private void buildAlias(String indexName, String aliasName, List<Alias> targets, IndicesAliasesRequestBuilder request) {
		BoolFilterBuilder filter = new BoolFilterBuilder();
		List<String> routing = Lists.newArrayList();
		for (Alias target : targets) {
			SearchBuilderSupport search = new EventSearchBuilder().addConstraint(Event.BUCKET.getName() + ":" + target.getId());
			routing.add(target.getId());
			if (target.getFilter() != null) {
				search.addConstraints(target.getFilter());
			}
			filter.should(search.buildFilter());
		}
		request.addAliasAction(AliasAction.newAddAliasAction(indexName, aliasName)
			.filter(filter)
			.indexRouting(Iterables.get(routing, 0))
			.searchRouting(Joiner.on(',').join(routing)));
	}

	public void deleteAlias(String indexName, String aliasName) {
		IndicesAliasesRequestBuilder request = client.admin().indices().prepareAliases().removeAlias(indexName, aliasName);
		IndicesAliasesResponse response = request.get();
		Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of alias deletion: %s", aliasName);
	}

	@Override
	public void close() {
		flush();
		client.close();
		node.close();
	}

	private void flush() {
		client.admin().indices().prepareFlush("_all").execute().actionGet();
	}
}
