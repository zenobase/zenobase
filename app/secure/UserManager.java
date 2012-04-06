package secure;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import play.Logger;
import services.IndexManager;
import services.NodeManager;

import com.google.common.base.Preconditions;
import common.Callback;
import common.Nodes;
import common.PartialList;

public class UserManager {

	private static final String INDEX_NAME = "users";

	private final IndexManager index;

	@Inject
	public UserManager(NodeManager node) {
		this.index = node.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating user index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(User.getSchema());
		}
	}

	public User find(Identity identity) {
		SearchHits hits = index.search(identityEquals(identity)).hits();
		Preconditions.checkState(hits.totalHits() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.totalHits());
		return hits.totalHits() > 0L ?
			User.parse(Nodes.read(hits.getAt(0).source())) : null;
	}

	public PartialList<User> find(int offset, int limit) {
		List<User> users = Lists.newArrayList();
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(QueryBuilders.matchAllQuery())
			.from(offset).size(limit)
			.sort(User.NAME.getName());
		SearchHits hits = index.search(search).hits();
		for (SearchHit hit : hits) {
			users.add(User.parse(Nodes.read(hit.source())));
		}
		return new PartialList<User>(users, hits.totalHits());
	}

	public void find(final Callback<User> callback) {
		index.search(QueryBuilders.matchAllQuery(), new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode object) {
				callback.call(User.parse(object));
			}
		});
	}

	private QueryBuilder identityEquals(Identity identity) {
		return QueryBuilders.termQuery(User.IDENTITY.getName(), identity);
	}

	private QueryBuilder isSuperuser(boolean b) {
		return QueryBuilders.termQuery(User.SUPERUSER.getName(), true);
	}

	public User find(String name) {
		ObjectNode object = index.get(User.TYPE_NAME, name);
		return object != null ? User.parse(object) : null;
	}

	public boolean isSuperuser(Identity identity) {
		QueryBuilder query = QueryBuilders.boolQuery()
			.must(identityEquals(identity))
			.must(isSuperuser(true));
		SearchHits hits = index.search(query).hits();
		Preconditions.checkState(hits.totalHits() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.totalHits());
		return hits.totalHits() > 0L;
	}

	public void store(User user) {
		index.store(User.TYPE_NAME, user.getName(), user.toJson(), true);
	}

	public void update(User user) {
		index.update(User.TYPE_NAME, user.getName(), user.toJson(), true);
	}

	public void delete(User user) {
		index.delete(User.TYPE_NAME, user.getName());
	}

	public boolean isEmpty() {
		return index.count() == 0L;
	}
}
