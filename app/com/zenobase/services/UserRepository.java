package com.zenobase.services;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import com.google.common.primitives.Ints;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
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
		PartialList<ObjectNode> hits = index.find(identityEquals(identity));
		Preconditions.checkState(hits.size() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.size());
		return hits.size() > 0L ?
			new User(hits.getElements().get(0)) : null;
	}

	public UserList find(int offset, int limit) {
		List<User> users = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(QueryBuilders.matchAllQuery())
			.sort(User.CREATED.getName(), SortOrder.ASC)
			.from(offset).size(limit)
			.version(true);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			users.add(new User(hit));
		}
		return new UserList(users, hits.size());
	}

	public void find(final Callback<User> callback) {
		index.find(QueryBuilders.matchAllQuery(), new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(new User(node));
			}
		}, 10);
	}

	private QueryBuilder identityEquals(Identity identity) {
		return QueryBuilders.termQuery(User.ID.getName(), identity.getId());
	}

	private QueryBuilder isSuperuser() {
		return QueryBuilders.termQuery(User.SUPERUSER.getName(), true);
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
			.must(identityEquals(identity))
			.must(isSuperuser());
		PartialList<ObjectNode> hits = index.find(query);
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

	public int count() {
		return Ints.checkedCast(index.count());
	}

	public boolean isEmpty() {
		return index.count() == 0L;
	}
}
