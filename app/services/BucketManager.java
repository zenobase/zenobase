package services;

import java.util.Map;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;
import org.elasticsearch.search.SearchHits;

import play.Logger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;

public class BucketManager {

	private static final String INDEX_NAME = "buckets";

	private final NodeManager manager;
	private final IndexManager buckets;

	@Inject
	public BucketManager(NodeManager manager) {
		this.manager = manager;
		this.buckets = manager.getIndex(INDEX_NAME);
		if (!buckets.exists()) {
			Logger.info("Creating bucket index...");
			buckets.create(1, 0);
			buckets.putMapping(Bucket.TYPE_NAME, Bucket.getSchema());
		}
	}

	public void store(Bucket bucket, boolean createIndex) {
		IndexManager index = manager.getIndex(bucket.getId());
		if (index.exists()) {
			if (createIndex) {
				throw new IllegalStateException("Index exists already: " + bucket.getId());
			}
		}
		else {
			if (createIndex) {
				index.create(1, 0);
				index.putMapping(Event.TYPE_NAME, Event.getSchema());
			}
			else {
				throw new IllegalStateException("Can't find index: " + bucket.getId());
			}
		}
		buckets.index(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public void deleteBucket(String id, String user) {
		QueryBuilder query = QueryBuilders.boolQuery()
			.must(QueryBuilders.fieldQuery(Bucket.ID.getName(), id))
			.must(QueryBuilders.fieldQuery(Bucket.USER.getName(), user));
		buckets.delete(query);
	}

	public Bucket findBucket(String bucketId, String user) {
		QueryBuilder query = QueryBuilders.boolQuery()
			.must(QueryBuilders.fieldQuery(Bucket.ID.getName(), bucketId))
			.must(QueryBuilders.fieldQuery(Bucket.USER.getName(), user));
		SearchHits hits = buckets.search(buckets.prepareSearch(query, null, 0, 10)).getHits();
		if (hits.totalHits() > 1) {
			Logger.warn("Expected a single match for bucket %s for user %s but got %d", bucketId, user, hits.getTotalHits());
		}
		if (hits.totalHits() == 1) {
			return fromMap(hits.getAt(0).getSource());
		}
		return null;
	}

	public ImmutableList<Bucket> findParticipants(String bucketId) {
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		QueryBuilder query = QueryBuilders.fieldQuery(Bucket.ID.getName(), bucketId);
		for (SearchHit hit : this.buckets.search(this.buckets.prepareSearch(query, null, 0, Integer.MAX_VALUE)).getHits()) {
			buckets.add(fromMap(hit.getSource()));
		}
		return buckets.build();
	}

	public ImmutableList<Bucket> findBuckets() {
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		for (SearchHit hit : this.buckets.search(this.buckets.prepareSearch(QueryBuilders.matchAllQuery(), null, 0, 10)).getHits()) {
			buckets.add(fromMap(hit.getSource()));
		}
		return buckets.build();
	}

	private Bucket fromMap(Map<String, Object> map) {
		String id = (String) map.get(Bucket.ID.getName());
		Bucket bucket = new Bucket(manager.getIndex(id), id);
		bucket.setLabel(map.get(Bucket.LABEL.getName()).toString());
		bucket.setUser(map.get(Bucket.USER.getName()).toString());
		bucket.setRole(map.get(Bucket.ROLE.getName()).toString());
		return bucket;
	}
}
