package services;

import java.util.List;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import play.Logger;
import schema.RoleType;
import secure.Identity;
import secure.Role;

import com.google.common.collect.ImmutableList;
import common.Callback;
import common.Nodes;
import common.PartialList;

public class BucketManager {

	private static final String INDEX_NAME = "buckets";

	private final NodeManager manager;
	private final IndexManager index;

	@Inject
	public BucketManager(NodeManager manager) {
		this.manager = manager;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating bucket index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Bucket.TYPE_NAME, Bucket.getSchema());
		}
	}

	public void store(Bucket bucket, boolean createIndex) {
		IndexManager index = manager.getIndex(bucket.getId());
		if (index.exists()) {
			if (createIndex) {
				throw new IllegalStateException("Index exists already: " + bucket.getId());
			}
			else {
				index.open();
			}
		}
		else {
			if (createIndex) {
				index.create(1);
				index.putMapping(Event.TYPE_NAME, Event.getSchema());
			}
			else {
				throw new IllegalStateException("Can't find index: " + bucket.getId());
			}
		}
		this.index.store(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public void deleteBucket(String id) {
		index.delete(QueryBuilders.termQuery(Bucket.ID.getName(), id));
		manager.getIndex(id).close();
	}

	public Bucket findBucket(String bucketId) {
		ObjectNode object = index.get(Bucket.TYPE_NAME, bucketId);
		return object != null ? parse(object) : null;
	}

	public ImmutableList<Bucket> findParticipants(String bucketId) {
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		QueryBuilder query = QueryBuilders.termQuery(Bucket.ID.getName(), bucketId);
		for (SearchHit hit : this.index.search(query).getHits()) {
			buckets.add(parse(hit.source()));
		}
		return buckets.build();
	}

	public PartialList<Bucket> findBuckets(int offset, int limit) {
		return findBuckets(QueryBuilders.matchAllQuery(), offset, limit);
	}

	public PartialList<Bucket> findBuckets(Identity identity, int offset, int limit) {
		return findBuckets(queryFor(identity), offset, limit);
	}

	private static QueryBuilder queryFor(Identity identity) {
		return QueryBuilders.nestedQuery(Bucket.ROLE.getName(), 
			QueryBuilders.termQuery(RoleType.IDENTITY.getName(), identity.getId()));
	}

	private PartialList<Bucket> findBuckets(QueryBuilder query, int offset, int limit) {
		List<Bucket> buckets = Lists.newArrayList();
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).from(offset).size(limit);
			// TODO .sort(Bucket.CREATED.getName());
		SearchHits hits = index.search(search).hits();
		for (SearchHit hit : hits) {
			buckets.add(parse(hit.source()));
		}
		return new PartialList<Bucket>(buckets, hits.getTotalHits());
	}

	public void findBuckets(final Callback<Bucket> callback) {
		findBuckets(QueryBuilders.matchAllQuery(), callback);
	}

	public void findBuckets(Identity identity, final Callback<Bucket> callback) {
		findBuckets(queryFor(identity), callback);
	}

	public void findBuckets(QueryBuilder query, final Callback<Bucket> callback) {
		index.search(query, new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode object) {
				callback.call(parse(object));
			}
		});
	}

	private Bucket parse(byte[] source) {
		return parse(Nodes.read(source));
	}

	private Bucket parse(ObjectNode object) {
		String id = object.get(Bucket.ID.getName()).asText();
		Bucket bucket = new Bucket(manager.getIndex(id), id);
		bucket.setLabel(object.get(Bucket.LABEL.getName()).asText());
		for (Role role : Bucket.ROLE.getType().get(object, Bucket.ROLE.getName())) {
			bucket.addRole(role);
		}
		return bucket;
	}
}
