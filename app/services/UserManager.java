package services;

import java.util.List;

import javax.inject.Inject;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import play.Logger;

import com.google.common.base.Preconditions;
import common.Callback;
import common.PartialList;

public class UserManager {

	private static final String INDEX_NAME = "users";

	private final Index index;

	@Inject
	public UserManager(IndexManager node) {
		this.index = node.getIndex(INDEX_NAME);
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

	public PartialList<User> find(int offset, int limit) {
		List<User> users = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(QueryBuilders.matchAllQuery())
			.from(offset).size(limit)
			.sort(User.NAME.getName());
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode node : hits.getElements()) {
			users.add(new User(node));
		}
		return new PartialList<User>(users, hits.size());
	}

	public void find(final Callback<User> callback) {
		index.find(QueryBuilders.matchAllQuery(), new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(new User(node));
			}
		});
	}

	private QueryBuilder identityEquals(Identity identity) {
		return QueryBuilders.termQuery(User.ID.getName(), identity.getId());
	}

	private QueryBuilder isSuperuser(boolean b) {
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
			.must(isSuperuser(true));
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

	public void delete(User user) {
		index.delete(User.TYPE_NAME, user.getName(), true);
	}

	public boolean isEmpty() {
		return index.count() == 0L;
	}
}
