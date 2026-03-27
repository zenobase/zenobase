package com.zenobase.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;

public class AuthorizationRepository extends RepositorySupport<Authorization> {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationRepository.class);

	static final String INDEX_NAME = "authorizations";

	private final Index index;

	@Inject
	public AuthorizationRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating authorization index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Authorization.getSchema());
		}
	}

	public void store(Authorization authorization, DateTime timestamp) {
		this.index.store(authorization.getId(), authorization.toJson(), true);
	}

	public boolean delete(String authId) {
		return index.delete(authId, true);
	}

	public @Nullable Authorization find(String authId) {
		ObjectNode node = index.get(authId);
		return node != null ? new Authorization(node) : null;
	}

	public PartialList<Authorization> find(int offset, int limit) {
		return find(new AuthorizationQuery(), offset, limit);
	}

	public PartialList<Authorization> find(AuthorizationQuery query, int offset, int limit) {
		SearchRequest request = SearchRequest.of(s -> s.index(index.getIndexName())
				.query(query.build())
				.version(true)
				.seqNoPrimaryTerm(true)
				.from(offset)
				.size(limit)
				.trackTotalHits(t -> t.enabled(true))
				.sort(so ->
						so.field(f -> f.field(Authorization.CREATED.getName()).order(SortOrder.Desc))));
		return new AuthorizationList(index.find(request));
	}

	public void find(AuthorizationQuery query, Callback<Authorization> callback) {
		super.find(query.build(), callback);
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected Authorization toObject(ObjectNode node) {
		return new Authorization(node);
	}
}
