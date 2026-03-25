package com.zenobase.services;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.opensearch.client.opensearch._types.SortOrder;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.joda.time.DateTime;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserList;

public class UserRepository extends RepositorySupport<User> {

	private static final Logger logger = LoggerFactory.getLogger(UserRepository.class);

	static final String INDEX_NAME = "users";

	private final Index index;

	@Inject
	public UserRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating user index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(User.getSchema());
		}
	}

	public void store(User user, DateTime timestamp) {
		index.store(user.getName(), user.toJson(), timestamp, true);
	}

	public void update(User user, DateTime timestamp) {
		index.update(user.getName(), user.toJson(), timestamp, true);
	}

	public boolean delete(User user) {
		return index.delete(user.getName(), true);
	}

	public User find(String name) {
		ObjectNode node = index.get(name);
		return node != null ? new User(node) : null;
	}

	public User find(Identity identity) {
		return Iterables.getOnlyElement(find(new UserQuery().principalEqualTo(identity), 0, 1), null);
	}

	public PartialList<User> find(UserQuery query, int offset, int limit) {
		SearchRequest request = SearchRequest.of(s -> s
			.index(index.getIndexName())
			.query(query.build())
			.sort(so -> so.field(f -> f.field(User.NAME.getName()).order(SortOrder.Asc)))
			.from(offset).size(limit)
			.trackTotalHits(t -> t.enabled(true))
			.version(true)
			.seqNoPrimaryTerm(true)
		);
		return new UserList(index.find(request));
	}

	public void find(Callback<User> callback) {
		super.find(new UserQuery().build(), callback);
	}

	public boolean isSuperuser(Identity identity) {
		var query = new UserQuery().principalEqualTo(identity).isSuperuser(true);
		PartialList<User> users = find(query, 0, 1);
		return !users.isEmpty();
	}

	public boolean exists(String name) {
		return index.exists(name);
	}

	public long size() {
		return index.count();
	}

	public boolean isEmpty() {
		return size() == 0L;
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected User toObject(ObjectNode node) {
		return new User(node);
	}
}
