package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsList;

public class CredentialsRepository extends RepositorySupport<Credentials> {

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

	public Credentials find(Identity principal, String type) {
		CredentialsQuery query = new CredentialsQuery().principalEqualTo(principal).typeEqualTo(type);
		PartialList<Credentials> results = find(query, 0, 2);
		if (results.isEmpty() && "withings".equals(type)) {
			return find(principal, "nokia");
		}
		if (results.getTotal() > 1) {
			Logger.warn("Found duplicate {} credentials for {}", type, principal);
		}
		return Iterables.getFirst(results, null);
	}

	public PartialList<Credentials> find(int offset, int limit) {
		return find(new CredentialsQuery(), offset, limit);
	}

	public PartialList<Credentials> find(CredentialsQuery query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query.build()).version(true).from(offset).size(limit);
		query.order().apply(search);
		return new CredentialsList(index.find(search));
	}

	public void find(CredentialsQuery query, Callback<Credentials> callback) {
		find(query.build(), callback);
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected Credentials toObject(ObjectNode node) {
		return new Credentials(node);
	}
}
