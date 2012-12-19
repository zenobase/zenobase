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

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.json.RolesField;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class BucketRepository {

	static final String INDEX_NAME = "buckets";

	private final IndexManager manager;
	private final Index index;

	@Inject
	public BucketRepository(IndexManager manager) {
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
			Preconditions.checkState(!createIndex, "Index exists already: %s", bucket.getId());
			index.open();
		}
		else {
			Preconditions.checkState(createIndex, "Can't find index: %s", bucket.getId());
			index.create(1);
			index.putMapping(Event.getSchema());
		}
		this.index.store(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public void update(Bucket bucket) {
		index.update(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), true);
	}

	public boolean delete(String bucketId) {
		boolean deleted = index.delete(Bucket.TYPE_NAME, bucketId, true);
		if (deleted) {
			manager.getIndex(bucketId).close();
		}
		return deleted;
	}

	public Bucket find(String bucketId) {
		ObjectNode node = index.get(Bucket.TYPE_NAME, bucketId);
		return node != null ? new Bucket(node) : null;
	}

	public BucketList findAll(int offset, int limit) {
		return find(QueryBuilders.matchAllQuery(), offset, limit);
	}

	public BucketList findBuckets(Identity identity, int offset, int limit) {
		return find(queryFor(identity), offset, limit);
	}

	private static QueryBuilder queryFor(Identity identity) {
		return QueryBuilders.nestedQuery(Bucket.ROLES.getName(),
			QueryBuilders.termQuery(RolesField.PRINCIPAL.getName(), identity.getId()));
	}

	private BucketList find(QueryBuilder query, int offset, int limit) {
		List<Bucket> buckets = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Bucket.CREATED.getName(), SortOrder.DESC);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			buckets.add(new Bucket(hit));
		}
		return new BucketList(buckets, hits.size(), new EventRepository(manager));
	}

	public void findAll(final Callback<Bucket> callback) {
		find(QueryBuilders.matchAllQuery(), callback);
	}

	public void findBuckets(Identity identity, final Callback<Bucket> callback) {
		find(queryFor(identity), callback);
	}

	private void find(QueryBuilder query, final Callback<Bucket> callback) {
		index.find(query, new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(new Bucket(node));
			}
		}, 10);
	}
}
