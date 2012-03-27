package secure;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import services.IndexManager;
import services.NodeManager;

import com.google.common.base.Preconditions;
import common.Nodes;

public class UserManager {

	private static final String INDEX_NAME = "users";

	private final IndexManager index;

	@Inject
	public UserManager(NodeManager node) {
		this.index = node.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating user index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(User.TYPE_NAME, User.getSchema());
		}
	}

	public User find(Identity identity) {
		SearchHits hits = index.search(identityEquals(identity)).hits();
		Preconditions.checkState(hits.totalHits() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.totalHits());
		return hits.totalHits() > 0L ?
			User.parse(Nodes.read(hits.getAt(0).source())) : null;
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
		index.index(User.TYPE_NAME, user.getName(), user.toJson(), true);
	}

	public void delete(User user) {
		index.delete(User.TYPE_NAME, user.getName());
	}

	public boolean isEmpty() {
		return index.count() == 0L;
	}
}
