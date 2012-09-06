package com.zenobase.services;

import java.util.List;

import javax.inject.Inject;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.action.search.SearchResponse;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.facet.FacetBuilders;
import org.elasticsearch.search.facet.terms.TermsFacet;
import org.elasticsearch.search.facet.terms.TermsFacet.ComparatorType;
import org.elasticsearch.search.sort.SortOrder;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.json.PermissionField;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.search.EventSearch;

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

	public boolean deleteBucket(String bucketId) {
		boolean deleted = index.delete(Bucket.TYPE_NAME, bucketId, true);
		if (deleted) {
			getIndex(bucketId).close();
		}
		return deleted;
	}

	public Bucket findBucket(String bucketId) {
		ObjectNode node = index.get(Bucket.TYPE_NAME, bucketId);
		return node != null ? new Bucket(node) : null;
	}

	public BucketList findBuckets(int offset, int limit) {
		return findBuckets(QueryBuilders.matchAllQuery(), offset, limit);
	}

	public BucketList findBuckets(Identity identity, int offset, int limit) {
		return findBuckets(queryFor(identity), offset, limit);
	}

	private static QueryBuilder queryFor(Identity identity) {
		return QueryBuilders.nestedQuery(Bucket.PERMISSIONS.getName(),
			QueryBuilders.termQuery(PermissionField.PRINCIPAL.getName(), identity.getId()));
	}

	private BucketList findBuckets(QueryBuilder query, int offset, int limit) {
		List<Bucket> buckets = Lists.newArrayListWithCapacity(limit);
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Bucket.CREATED.getName(), SortOrder.DESC);
		PartialList<ObjectNode> hits = index.find(search);
		for (ObjectNode hit : hits.getElements()) {
			buckets.add(new Bucket(hit));
		}
		return new BucketList(buckets, hits.size(), this);
	}

	public void findBuckets(final Callback<Bucket> callback) {
		findBuckets(QueryBuilders.matchAllQuery(), callback);
	}

	public void findBuckets(Identity identity, final Callback<Bucket> callback) {
		findBuckets(queryFor(identity), callback);
	}

	private void findBuckets(QueryBuilder query, final Callback<Bucket> callback) {
		index.find(query, new Callback<ObjectNode>() {
			@Override
			public void call(ObjectNode node) {
				callback.call(new Bucket(node));
			}
		}, 10);
	}

	public void add(String bucketId, Event event) {
		event.prePersist();
		getIndex(bucketId).store(Event.TYPE_NAME, event.getId(), event.toJson(), false);
	}

	public void update(String bucketId, Event event) {
		event.prePersist();
		getIndex(bucketId).update(Event.TYPE_NAME, event.getId(), event.toJson(), false);
	}

	public boolean delete(String bucketId, String eventId) {
		return getIndex(bucketId).delete(Event.TYPE_NAME, eventId, false);
	}

	public Event findEvent(String bucketId, String eventId) {
		ObjectNode node = getIndex(bucketId).get(Event.TYPE_NAME, eventId);
		return node != null ? new Event(node) : null;
	}

	public ObjectNode findEvents(String bucketId, EventSearch search) {
		return search.execute(manager.getIndex(bucketId));
	}

	public List<String> terms(String bucketId, String field) {
		final int limit = 100;
		final String facetId = "terms";
		SearchSourceBuilder search = new SearchSourceBuilder().field(field)
			.facet(FacetBuilders.termsFacet(facetId).field(field).size(limit).order(ComparatorType.COUNT));
		SearchResponse response = manager.getIndex(bucketId).search(search);
		List<String> terms = Lists.newArrayList();
		TermsFacet facet = response.facets().facet(TermsFacet.class, facetId);
		for (TermsFacet.Entry entry : facet.entries()) {
			terms.add(entry.getTerm());
		}
		return terms;
	}

	public long getSize(String bucketId) {
		return getIndex(bucketId).count();
	}

	private Index getIndex(String bucketId) {
		return manager.getIndex(bucketId);
	}

	public void refresh(String bucketId) {
		getIndex(bucketId).refresh();
	}
}
