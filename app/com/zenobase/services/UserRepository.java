package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Iterables;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserList;

public class UserRepository extends RepositorySupport<User> {

	static final String INDEX_NAME = "users";

	private final Index index;

	@Inject
	public UserRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating user index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(User.getSchema());
		}
	}

	public void store(User user, DateTime timestamp) {
		index.store(User.TYPE_NAME, user.getName(), user.toJson(), timestamp, true);
	}

	public void update(User user, DateTime timestamp) {
		index.update(User.TYPE_NAME, user.getName(), user.toJson(), timestamp, true);
	}

	public boolean delete(User user) {
		return index.delete(User.TYPE_NAME, user.getName(), true);
	}

	public User find(String name) {
		ObjectNode node = index.get(User.TYPE_NAME, name);
		return node != null ? new User(node) : null;
	}

	public User find(Identity identity) {
		return Iterables.getOnlyElement(find(new UserQuery().principalEqualTo(identity), 0, 1), null);
	}

	public PartialList<User> find(UserQuery query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query.build())
			.sort(User.NAME.getName(), SortOrder.ASC)
			.from(offset).size(limit)
			.version(true);
		return new UserList(index.find(search));
	}

	public void find(UserQuery query, Callback<User> callback) {
		super.find(query.build(), callback);
	}

	public void find(Callback<User> callback) {
		super.find(new UserQuery().build(), callback);
	}

	public boolean isSuperuser(Identity identity) {
		UserQuery query = new UserQuery().principalEqualTo(identity).isSuperuser(true);
		PartialList<User> users = find(query, 0, 1);
		return !users.isEmpty();
	}

	public boolean exists(String name) {
		return index.exists(User.TYPE_NAME, name);
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
