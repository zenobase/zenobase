package com.zenobase.services;

import java.io.Closeable;
import java.io.IOException;
import java.util.List;

import jakarta.inject.Inject;
import jakarta.inject.Named;

import jakarta.json.Json;
import jakarta.json.JsonObjectBuilder;

import java.util.concurrent.TimeUnit;

import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.util.concurrent.Uninterruptibles;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import org.opensearch.client.opensearch.OpenSearchClient;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch.generic.Requests;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.models.Alias;
import com.zenobase.models.Event;
import com.zenobase.search.EventSearchBuilder;
import com.zenobase.search.SearchBuilderSupport;

public class IndexManager implements Closeable {

	private static final Logger logger = LoggerFactory.getLogger(IndexManager.class);

	private final OpenSearchClient client;
	private final String snapshotRepository;

	@Inject
	public IndexManager(ClientFactory clientFactory, @Named("opensearch.snapshot.bucket") String snapshotBucket, @Named("aws.region") String snapshotRegion, @Named("opensearch.snapshot_role_arn") String snapshotRoleArn) {
		client = clientFactory.createClient();
		var cluster = new Cluster(client);
		while (!cluster.isReady()) {
			logger.warn("Waiting for cluster to recover...");
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.SECONDS);
		}
		cluster.disableAutoCreateIndex();
		if (!snapshotBucket.isEmpty()) {
			String clusterName = new Cluster(client).getHealth().clusterName();
			String repositoryName = clusterName.contains(":") ? clusterName.substring(clusterName.indexOf(':') + 1) : clusterName;
			registerSnapshotRepository(repositoryName, snapshotBucket, snapshotRegion, snapshotRoleArn);
			this.snapshotRepository = repositoryName;
		} else {
			registerLocalSnapshotRepository("snapshots", "snapshots");
			this.snapshotRepository = "snapshots";
		}
	}

	public IndexManager(ClientFactory clientFactory) {
		client = clientFactory.createClient();
		this.snapshotRepository = "";
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

	private void registerSnapshotRepository(String repositoryName, String bucket, String region, String roleArn) {
		try {
			JsonObjectBuilder settings = Json.createObjectBuilder()
				.add("bucket", bucket)
				.add("base_path", repositoryName)
				.add("region", region)
				.add("shard_path_type", "FIXED");
			if (!roleArn.isEmpty()) {
				settings.add("role_arn", roleArn);
			}
			client.generic().execute(
				Requests.builder()
					.endpoint("/_snapshot/" + repositoryName)
					.method("PUT")
					.json(Json.createObjectBuilder()
						.add("type", "s3")
						.add("settings", settings))
					.build()
			).close();
			logger.info("Registered snapshot repository: {} (s3)", repositoryName);
		} catch (IOException e) {
			throw new RuntimeException("Failed to register snapshot repository: " + repositoryName, e);
		}
	}

	private void registerLocalSnapshotRepository(String repositoryName, String location) {
		try {
			client.generic().execute(
				Requests.builder()
					.endpoint("/_snapshot/" + repositoryName)
					.method("PUT")
					.json(Json.createObjectBuilder()
						.add("type", "fs")
						.add("settings", Json.createObjectBuilder()
							.add("location", location)))
					.build()
			).close();
			logger.info("Registered snapshot repository: {} (fs)", repositoryName);
		} catch (IOException e) {
			throw new RuntimeException("Failed to register local snapshot repository: " + repositoryName, e);
		}
	}

	public void createAlias(String indexName, String aliasName, List<Alias> targets) {
		try {
			boolean acknowledged = client.indices().updateAliases(u -> {
				buildAlias(indexName, aliasName, targets, u);
				return u;
			}).acknowledged();
			Preconditions.checkState(acknowledged, "Expected acknowledgement of alias creation: %s", aliasName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void updateAlias(String indexName, String aliasName, List<Alias> targets) {
		Preconditions.checkArgument(!targets.isEmpty(), "Can't remove all aliases from %s", aliasName);
		try {
			boolean acknowledged = client.indices().updateAliases(u -> {
				u.actions(a -> a.remove(r -> r.index(indexName).alias(aliasName)));
				buildAlias(indexName, aliasName, targets, u);
				return u;
			}).acknowledged();
			Preconditions.checkState(acknowledged, "Expected acknowledgement of alias update: %s", aliasName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private void buildAlias(String indexName, String aliasName, List<Alias> targets,
			org.opensearch.client.opensearch.indices.UpdateAliasesRequest.Builder u) {
		List<Query> shoulds = Lists.newArrayList();
		List<String> routing = Lists.newArrayList();
		for (Alias target : targets) {
			SearchBuilderSupport search = new EventSearchBuilder().addConstraint(Event.BUCKET.getName() + ":" + target.id());
			routing.add(target.id());
			if (target.filter() != null) {
				search.addConstraints(target.filter());
			}
			shoulds.add(search.buildFilter());
		}
		Query filter = Query.of(q -> q.bool(b -> b.should(shoulds)));
		String indexRouting = Iterables.get(routing, 0);
		String searchRouting = Joiner.on(',').join(routing);
		u.actions(a -> a.add(add -> add
			.index(indexName)
			.alias(aliasName)
			.filter(filter)
			.indexRouting(indexRouting)
			.searchRouting(searchRouting)
		));
	}

	public void deleteAlias(String indexName, String aliasName) {
		try {
			boolean acknowledged = client.indices().updateAliases(u -> u
				.actions(a -> a.remove(r -> r.index(indexName).alias(aliasName)))
			).acknowledged();
			Preconditions.checkState(acknowledged, "Expected acknowledgement of alias deletion: %s", aliasName);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void close() {
		try {
			client._transport().close();
		} catch (Exception e) {
			logger.error("Error closing client", e);
		}
	}
}
