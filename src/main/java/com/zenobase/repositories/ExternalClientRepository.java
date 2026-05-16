package com.zenobase.repositories;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.ExternalClientList;
import com.zenobase.models.Identity;
import com.zenobase.queries.ExternalClientQuery;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalClientRepository extends RepositorySupport<ExternalClient> {

	private static final Logger logger = LoggerFactory.getLogger(ExternalClientRepository.class);

	static final String INDEX_NAME = "external_clients";

	private final Index index;

	@Inject
	public ExternalClientRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating external client index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(ExternalClient.SCHEMA);
		}
	}

	public void store(ExternalClient client) {
		index.store(client, true);
	}

	public void update(ExternalClient client) {
		index.update(client, true);
	}

	public boolean delete(Identity user, Identity client) {
		return index.delete(ExternalClient.id(user, client), true);
	}

	public @Nullable ExternalClient find(String id) {
		ObjectNode node = index.get(id);
		return node != null ? toObject(node) : null;
	}

	public @Nullable ExternalClient find(Identity user, Identity client) {
		return find(ExternalClient.id(user, client));
	}

	public void find(ExternalClientQuery query, Callback<ExternalClient> callback) {
		super.find(query.build(), ExternalClientQuery.DEFAULT_ORDER, callback);
	}

	public PartialList<ExternalClient> find(ExternalClientQuery query, int offset, int limit) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
			.index(index.getIndexName())
			.query(query.build())
			.version(true)
			.seqNoPrimaryTerm(true)
			.from(offset)
			.size(limit)
			.trackTotalHits(t -> t.enabled(true));
		ExternalClientQuery.DEFAULT_ORDER.apply(builder);
		return new ExternalClientList(index.find(builder.build()));
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected ExternalClient toObject(ObjectNode node) {
		return new ExternalClient(node);
	}
}
