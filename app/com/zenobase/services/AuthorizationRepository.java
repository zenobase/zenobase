package com.zenobase.services;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import com.google.common.collect.Lists;

import com.zenobase.common.PartialList;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;

public class AuthorizationRepository {

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

	public void store(Authorization authorization) {
		this.index.store(Authorization.TYPE_NAME, authorization.getId(), authorization.toJson(), true);
	}

	public boolean delete(String authId) {
		return index.delete(Authorization.TYPE_NAME, authId, true);
	}

	public Authorization find(String authId) {
		ObjectNode node = index.get(Authorization.TYPE_NAME, authId);
		return node != null ? new Authorization(node) : null;
	}

	public AuthorizationList find(int offset, int limit) {
		return find(QueryBuilders.matchAllQuery(), offset, limit);
	}

	public AuthorizationList find(String field, String value, boolean clientOnly, int offset, int limit) {
		return find(queryFor(field, value, clientOnly), offset, limit);
	}

	private static QueryBuilder queryFor(String field, String value, boolean clientOnly) {
		QueryBuilder query = QueryBuilders.termQuery(field, value);
		if (clientOnly) {
			query = QueryBuilders.filteredQuery(query, FilterBuilders.existsFilter(Authorization.CLIENT.getName()));
		}
		return query;
	}

	private AuthorizationList find(QueryBuilder query, int offset, int limit) {
		List<Authorization> authorizations = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Authorization.CREATED.getName(), SortOrder.DESC);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			authorizations.add(new Authorization(hit));
		}
		return new AuthorizationList(authorizations, hits.size());
	}
}
