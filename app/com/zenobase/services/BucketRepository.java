package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.query.QueryBuilder;
import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.elasticsearch.search.sort.SortOrder;
import org.joda.time.DateTime;
import play.Logger;
import com.google.common.base.Preconditions;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.json.RolesField;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class BucketRepository extends RepositorySupport<Bucket> {

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

	public void store(Bucket bucket, DateTime timestamp, boolean createIndex) {
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
		this.index.store(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), timestamp, true);
	}

	public void update(Bucket bucket, DateTime timestamp) {
		index.update(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), timestamp, true);
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
		return node != null ? toObject(node) : null;
	}

	public PartialList<Bucket> find(int offset, int limit) {
		return find(QueryBuilders.matchAllQuery(), offset, limit);
	}

	public PartialList<Bucket> find(Identity identity, int offset, int limit) {
		return find(restrict(identity), offset, limit);
	}

	private static QueryBuilder restrict(Identity identity) {
		return QueryBuilders.nestedQuery(Bucket.ROLES.getName(),
			QueryBuilders.termQuery(RolesField.PRINCIPAL.getName(), identity.getId()));
	}

	private PartialList<Bucket> find(QueryBuilder query, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query).version(true).from(offset).size(limit)
			.sort(Bucket.CREATED.getName(), SortOrder.DESC);
		return new BucketList(index.find(search));
	}

	public void find(Identity identity, final Callback<Bucket> callback) {
		find(restrict(identity), callback);
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected Bucket toObject(ObjectNode node) {
		return new Bucket(node);
	}
}
