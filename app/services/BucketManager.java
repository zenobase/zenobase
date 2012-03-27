package services;

import javax.inject.Inject;

import models.Bucket;
import models.Event;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.SearchHit;

import play.Logger;
import schema.RoleType;
import secure.Identity;
import secure.Role;

import com.google.common.collect.ImmutableList;
import common.Nodes;

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
			buckets.create(Integer.MAX_VALUE);
			buckets.putMapping(Bucket.TYPE_NAME, Bucket.getSchema());
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
		buckets.index(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public void deleteBucket(String id) {
		buckets.delete(QueryBuilders.termQuery(Bucket.ID.getName(), id));
		manager.getIndex(id).close();
	}

	public Bucket findBucket(String bucketId) {
		ObjectNode object = buckets.get(Bucket.TYPE_NAME, bucketId);
		return object != null ? parse(object) : null;
	}

	public ImmutableList<Bucket> findParticipants(String bucketId) {
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		QueryBuilder query = QueryBuilders.termQuery(Bucket.ID.getName(), bucketId);
		for (SearchHit hit : this.buckets.search(query).getHits()) {
			buckets.add(parse(hit.source()));
		}
		return buckets.build();
	}

	public ImmutableList<Bucket> findBuckets() {
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		for (SearchHit hit : this.buckets.search(QueryBuilders.matchAllQuery()).getHits()) {
			buckets.add(parse(hit.source()));
		}
		return buckets.build();
	}

	public ImmutableList<Bucket> findBuckets(Identity identity) {
		ImmutableList.Builder<Bucket> buckets = ImmutableList.builder();
		QueryBuilder query = QueryBuilders.nestedQuery(Bucket.ROLE.getName(), QueryBuilders.termQuery(RoleType.IDENTITY.getName(), identity.getId()));
		for (SearchHit hit : this.buckets.search(query).getHits()) {
			buckets.add(parse(hit.source()));
		}
		return buckets.build();
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
