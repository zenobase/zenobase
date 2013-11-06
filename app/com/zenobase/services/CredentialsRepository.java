package com.zenobase.services;

import javax.inject.Inject;

import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsList;

public class CredentialsRepository {

	static final String INDEX_NAME = "credentials";

	private final Index index;

	@Inject
	public CredentialsRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating credentials index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Credentials.getSchema());
		}
	}

	public void store(Credentials credentials, DateTime timestamp) {
		this.index.store(Credentials.TYPE_NAME, credentials.getId(), credentials.toJson(), timestamp, true);
	}

	public void update(Credentials credentials, DateTime timestamp) {
		index.update(Credentials.TYPE_NAME, credentials.getId(), credentials.toJson(), timestamp, true);
	}

	public boolean delete(String credentialsId) {
		return index.delete(Credentials.TYPE_NAME, credentialsId, false);
	}

	public Credentials find(String credentialsId) {
		ObjectNode node = index.get(Credentials.TYPE_NAME, credentialsId);
		return node != null ? new Credentials(node) : null;
	}

	public CredentialsList find(String field, String value, int offset, int limit) {
		return find(QueryBuilders.termQuery(field, value), offset, limit);
	}

	public Credentials find(Identity principal, String type) {
		QueryBuilder q = QueryBuilders.boolQuery()
			.must(QueryBuilders.termQuery(Credentials.PRINCIPAL.getName(), principal.getId()))
			.must(QueryBuilders.termQuery(Credentials.TYPE.getName(), type));
		return Iterables.getOnlyElement(find(q, 0, 2), null);
	}

	public CredentialsList find(int offset, int limit) {
		return find(QueryBuilders.matchAllQuery(), offset, limit);
	}

	private CredentialsList find(QueryBuilder query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Credentials.CREATED.getName(), SortOrder.DESC);
		return new CredentialsList(index.find(search));
	}
}
