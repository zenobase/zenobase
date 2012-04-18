package services;

import java.util.List;

import javax.inject.Inject;

import models.Bucket;
import models.Event;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;

import play.Logger;
import schema.PermissionField;

import common.Callback;
import common.PartialList;

public class BucketManager {

	private static final String INDEX_NAME = "buckets";

	private final IndexManager manager;
	private final Index index;

	@Inject
	public BucketManager(IndexManager manager) {
		this.manager = manager;
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			Logger.info("Creating bucket index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(Bucket.getSchema());
		}
	}

	public void store(Bucket bucket, boolean createIndex) {
		Index index = manager.getIndex(bucket.getId());
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
				index.putMapping(Event.getSchema());
			}
			else {
				throw new IllegalStateException("Can't find index: " + bucket.getId());
			}
		}
		this.index.store(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public void update(Bucket bucket) {
		index.update(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public void deleteBucket(String id) {
		index.delete(Bucket.TYPE_NAME, id, true);
		getIndex(id).close();
	}

	public Bucket findBucket(String bucketId) {
		ObjectNode node = index.get(Bucket.TYPE_NAME, bucketId);
		return node != null ? new Bucket(node) : null;
	}

	public PartialList<Bucket> findBuckets(int offset, int limit) {
		return findBuckets(QueryBuilders.matchAllQuery(), offset, limit);
	}

	public PartialList<Bucket> findBuckets(Identity identity, int offset, int limit) {
		return findBuckets(queryFor(identity), offset, limit);
	}

	private static QueryBuilder queryFor(Identity identity) {
		return QueryBuilders.nestedQuery(Bucket.PERMISSIONS.getName(), 
			QueryBuilders.termQuery(PermissionField.PRINCIPAL.getName(), identity.getId()));
	}

	private PartialList<Bucket> findBuckets(QueryBuilder query, int offset, int limit) {
		List<Bucket> buckets = Lists.newArrayList();
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).from(offset).size(limit);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			buckets.add(new Bucket(hit));
		}
		return new PartialList<Bucket>(buckets, hits.size());
	}

	public void findBuckets(final Callback<Bucket> callback) {
		findBuckets(QueryBuilders.matchAllQuery(), callback);
	}

	public void findBuckets(Identity identity, final Callback<Bucket> callback) {
		findBuckets(queryFor(identity), callback);
	}

	public void findBuckets(QueryBuilder query, final Callback<Bucket> callback) {
		index.find(query, new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(new Bucket(node));
			}
		});
	}

	public void add(String bucketId, Event event) {
		event.prePersist();
		getIndex(bucketId).store(Event.TYPE_NAME, event.getId(), event.toJson(), false);
	}

	public void delete(String bucketId, String eventId) {
		getIndex(bucketId).delete(Event.TYPE_NAME, eventId, false);
	}

	public Event findEvent(String bucketId, String eventId) {
		ObjectNode node = getIndex(bucketId).get(Event.TYPE_NAME, eventId);
		return node != null ? new Event(node) : null;
	}

	public long getSize(String bucketId) {
		return getIndex(bucketId).count();
	}

	private Index getIndex(String bucketId) {
		return manager.getIndex(bucketId);
	}
}
