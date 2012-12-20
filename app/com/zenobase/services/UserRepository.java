package com.zenobase.services;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import com.google.common.base.Preconditions;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.common.DefaultPartialList;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.models.UserList;

public class UserRepository {

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

	public User find(Identity identity) {
		DefaultPartialList<ObjectNode> hits = index.find(restrict(identity));
		Preconditions.checkState(hits.size() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.size());
		return hits.size() > 0L ?
			new User(hits.get(0)) : null;
	}

	public PartialList<User> find(int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(QueryBuilders.matchAllQuery())
			.sort(User.NAME.getName(), SortOrder.ASC)
			.from(offset).size(limit)
			.version(true);
		return new UserList(index.find(search));
	}

	public void find(final Callback<User> callback) {
		index.find(QueryBuilders.matchAllQuery(), new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(new User(node));
			}
		}, 10);
	}

	private static QueryBuilder restrict(Identity identity) {
		return QueryBuilders.termQuery(User.ID.getName(), identity.getId());
	}

	private static QueryBuilder restrict(String field, boolean value) {
		return QueryBuilders.termQuery(field, value);
	}

	public User find(String name) {
		ObjectNode node = index.get(User.TYPE_NAME, name);
		return node != null ? new User(node) : null;
	}

	public boolean exists(String name) {
		return index.exists(User.TYPE_NAME, name);
	}

	public boolean isSuperuser(Identity identity) {
		QueryBuilder query = QueryBuilders.boolQuery()
			.must(restrict(identity))
			.must(restrict(User.SUPERUSER.getName(), true));
		DefaultPartialList<ObjectNode> hits = index.find(query);
		Preconditions.checkState(hits.size() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.size());
		return hits.size() > 0L;
	}

	public void store(User user) {
		index.store(User.TYPE_NAME, user.getName(), user.toJson(), true);
	}

	public void update(User user) {
		index.update(User.TYPE_NAME, user.getName(), user.toJson(), true);
	}

	public boolean delete(User user) {
		return index.delete(User.TYPE_NAME, user.getName(), true);
	}

	public long size() {
		return index.count();
	}

	public boolean isEmpty() {
		return size() == 0L;
	}
}
