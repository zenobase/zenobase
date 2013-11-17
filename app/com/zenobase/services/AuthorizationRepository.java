package com.zenobase.services;

import javax.inject.Inject;

import org.elasticsearch.index.query.BoolQueryBuilder;
import org.elasticsearch.index.query.FilterBuilder;
import org.elasticsearch.index.query.FilterBuilders;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Period;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;

import com.zenobase.common.PartialList;
import com.zenobase.json.Field;
import com.zenobase.models.Identity;
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

	public Authorization find(Identity principal, Identity client, String scope) {
		return Iterables.getFirst(findAll(principal, client, scope), null);
	}

	public PartialList<Authorization> findAll(Identity principal, Identity client, String scope) {
		BoolQueryBuilder query = QueryBuilders.boolQuery();
		add(query, Authorization.PRINCIPAL, principal != null ? principal.getId() : null);
		add(query, Authorization.CLIENT, client != null ? client.getId() : null);
		add(query, Authorization.SCOPE, scope);
		return find(query, 0, 100);
	}

	private static BoolQueryBuilder add(BoolQueryBuilder query, Field<?> field, String value) {
		return value != null
			? query.must(QueryBuilders.termQuery(field.getName(), value))
			: query.mustNot(QueryBuilders.constantScoreQuery(FilterBuilders.existsFilter(field.getName())));
	}

	public PartialList<Authorization> find(int offset, int limit) {
		return find(QueryBuilders.matchAllQuery(), offset, limit);
	}

	/**
	 * Find authorizations for a user.
	 */
	public PartialList<Authorization> find(Identity principal, Boolean client, int offset, int limit) {
		QueryBuilder query = QueryBuilders.termQuery(Authorization.PRINCIPAL.getName(), principal.getId());
		if (client != null) {
			query = QueryBuilders.filteredQuery(query, existsOrMissingFilter(Authorization.CLIENT.getName(), client));
		}
		return find(query, offset, limit);
	}

	private static FilterBuilder existsOrMissingFilter(String field, boolean exists) {
		return exists ? FilterBuilders.existsFilter(field) : FilterBuilders.missingFilter(field);
	}

	/**
	 * Find authorizations without a client that are older than maxAge.
	 */
	public PartialList<Authorization> find(Period maxAge, int offset, int limit) {
		return find(QueryBuilders.filteredQuery(
			QueryBuilders.rangeQuery(Authorization.CREATED.getName()).to(DateTime.now(DateTimeZone.UTC).minus(maxAge)),
			FilterBuilders.missingFilter(Authorization.CLIENT.getName())), offset, limit);
	}

	private PartialList<Authorization> find(QueryBuilder query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Authorization.CREATED.getName(), SortOrder.DESC);
		return new AuthorizationList(index.find(search));
	}
}
