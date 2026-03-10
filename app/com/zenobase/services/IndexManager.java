package com.zenobase.services;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.opensearch.action.admin.indices.alias.IndicesAliasesRequest;
import org.opensearch.action.support.master.AcknowledgedResponse;
import org.opensearch.client.RestHighLevelClient;
import org.opensearch.index.query.BoolQueryBuilder;
import org.opensearch.index.query.QueryBuilder;
import play.Logger;

import com.zenobase.models.Alias;
import com.zenobase.models.Event;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.SearchBuilderSupport;

public class IndexManager implements Closeable {

	private final RestHighLevelClient client;
	private final String snapshotRepository;

	@Inject
	public IndexManager(ClientFactory clientFactory, @Named("es.snapshot.repository") String snapshotRepository) {
		this.snapshotRepository = snapshotRepository;
		client = clientFactory.createClient();
		while (!new Cluster(client).isReady()) {
			Logger.warn("Waiting for cluster to recover...");
		}
	}

	public IndexManager(ClientFactory clientFactory) {
		this(clientFactory, "");
	}

	public Index getIndex(String indexName) {
		return new Index(indexName, client);
	}

	public Cluster getCluster() {
		return new Cluster(client);
	}

	public SnapshotManager getSnapshotManager() {
		return new SnapshotManager(client, snapshotRepository);
	}

	public SnapshotManager getSnapshotManager(String repositoryName) {
		return new SnapshotManager(client, repositoryName);
	}

	public void createAlias(String indexName, String aliasName, List<Alias> targets) {
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		buildAlias(indexName, aliasName, targets, request);
		try {
			AcknowledgedResponse response = client.indices().updateAliases(request, TypeInjectingInterceptor.OPTIONS);
			Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of alias creation: %s", aliasName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateAlias(String indexName, String aliasName, List<Alias> targets) {
		Preconditions.checkArgument(!targets.isEmpty(), "Can't remove all aliases from %s", aliasName);
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
			.index(indexName).alias(aliasName));
		buildAlias(indexName, aliasName, targets, request);
		try {
			AcknowledgedResponse response = client.indices().updateAliases(request, TypeInjectingInterceptor.OPTIONS);
			Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of alias update: %s", aliasName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void buildAlias(String indexName, String aliasName, List<Alias> targets, IndicesAliasesRequest request) {
		BoolQueryBuilder filter = new BoolQueryBuilder();
		List<String> routing = Lists.newArrayList();
		for (Alias target : targets) {
			SearchBuilderSupport search = new EventSearchBuilder().addConstraint(Event.BUCKET.getName() + ":" + target.getId());
			routing.add(target.getId());
			if (target.getFilter() != null) {
				search.addConstraints(target.getFilter());
			}
			filter.should(search.buildFilter());
		}
		request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.ADD)
			.index(indexName).alias(aliasName)
			.filter(filter)
			.indexRouting(Iterables.get(routing, 0))
			.searchRouting(Joiner.on(',').join(routing)));
	}

	public void deleteAlias(String indexName, String aliasName) {
		IndicesAliasesRequest request = new IndicesAliasesRequest();
		request.addAliasAction(new IndicesAliasesRequest.AliasActions(IndicesAliasesRequest.AliasActions.Type.REMOVE)
			.index(indexName).alias(aliasName));
		try {
			AcknowledgedResponse response = client.indices().updateAliases(request, TypeInjectingInterceptor.OPTIONS);
			Preconditions.checkState(response.isAcknowledged(), "Expected acknowledgement of alias deletion: %s", aliasName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			client.close();
		} catch (IOException e) {
			Logger.error("Error closing client", e);
		}
	}
}
