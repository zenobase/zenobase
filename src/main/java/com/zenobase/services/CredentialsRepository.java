package com.zenobase.services;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.CredentialsList;

public class CredentialsRepository extends RepositorySupport<Credentials> {

	private static final Logger logger = LoggerFactory.getLogger(CredentialsRepository.class);

	static final String INDEX_NAME = "credentials";

	private final Index index;

	@Inject
	public CredentialsRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating credentials index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Credentials.getSchema());
		}
	}

	public void store(Credentials credentials, DateTime timestamp) {
		this.index.store(credentials.getId(), credentials.toJson(), timestamp, true);
	}

	public void update(Credentials credentials, DateTime timestamp) {
		index.update(credentials.getId(), credentials.toJson(), timestamp, true);
	}

	public boolean delete(String credentialsId) {
		return index.delete(credentialsId, false);
	}

	public Credentials find(String credentialsId) {
		ObjectNode node = index.get(credentialsId);
		return node != null ? new Credentials(node) : null;
	}

	public Credentials find(Identity principal, String type) {
		var query = new CredentialsQuery().principalEqualTo(principal).typeEqualTo(type);
		PartialList<Credentials> results = find(query, 0, 2);
		if (results.getTotal() > 1) {
			logger.warn("Found duplicate {} credentials for {}", type, principal);
		}
		return Iterables.getFirst(results, null);
	}

	public PartialList<Credentials> find(int offset, int limit) {
		return find(new CredentialsQuery(), offset, limit);
	}

	public PartialList<Credentials> find(CredentialsQuery query, int offset, int limit) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
				.index(index.getIndexName())
				.query(query.build())
				.version(true)
				.seqNoPrimaryTerm(true)
				.from(offset)
				.size(limit)
				.trackTotalHits(t -> t.enabled(true));
		query.order().apply(builder);
		return new CredentialsList(index.find(builder.build()));
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
