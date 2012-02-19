package secure;

import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHits;

import play.Logger;
import services.IndexManager;
import services.NodeManager;

import com.google.common.base.Preconditions;

public class UserManager {

	private static final String INDEX_NAME = "users";

	private final IndexManager index;

	@Inject
	public UserManager(NodeManager node) {
		this.index = node.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating user index...");
			index.create(1, 0);
			index.putMapping(User.TYPE_NAME, User.getSchema());
		}
	}

	public User find(Identity identity) {
		SearchHits hits = index.search(QueryBuilders.termQuery(User.IDENTITY.getName(), identity)).hits();
		Preconditions.checkState(hits.totalHits() <= 1,
			"Expected 0..1 hits for identity '%s' but got %s", identity, hits.totalHits());
		return hits.totalHits() > 0L ?
			User.fromMap(hits.getAt(0).getSource()) : null;
	}

	public User find(String name) {
		SearchHits hits = index.search(QueryBuilders.termQuery(User.NAME.getName(), name)).hits();
		Preconditions.checkState(hits.totalHits() <= 1,
			"Expected 0..1 hits for name '%s' but got %s", name, hits.totalHits());
		return hits.totalHits() > 0L ?
			User.fromMap(hits.getAt(0).getSource()) : null;
	}

	public void store(User user) {
		index.index(User.TYPE_NAME, user.getName(), user.toJson(), true);
	}

	public void delete(User user) {
		index.delete(User.TYPE_NAME, user.getName());
	}
}
