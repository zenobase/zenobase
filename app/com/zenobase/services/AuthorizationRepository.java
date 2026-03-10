package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.opensearch.search.builder.SearchSourceBuilder;
import org.opensearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;

public class AuthorizationRepository extends RepositorySupport<Authorization> {

	static final String INDEX_NAME = "authorizations";

	private final Index index;

	@Inject
	public AuthorizationRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating authorization index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Authorization.getSchema());
		}
	}

	public void store(Authorization authorization, DateTime timestamp) {
		this.index.store(Authorization.TYPE_NAME, authorization.getId(), authorization.toJson(), timestamp, true);
	}

	public boolean delete(String authId) {
		return index.delete(Authorization.TYPE_NAME, authId, true);
	}

	public Authorization find(String authId) {
		ObjectNode node = index.get(Authorization.TYPE_NAME, authId);
		return node != null ? new Authorization(node) : null;
	}

	public PartialList<Authorization> find(int offset, int limit) {
		return find(new AuthorizationQuery(), offset, limit);
	}

	public PartialList<Authorization> find(AuthorizationQuery query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query.build()).version(true).from(offset).size(limit)
			.sort(Authorization.CREATED.getName(), SortOrder.DESC);
		return new AuthorizationList(index.find(search));
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
