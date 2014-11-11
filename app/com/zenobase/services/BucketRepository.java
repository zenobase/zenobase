package com.zenobase.services;

import javax.inject.Inject;

import org.elasticsearch.index.query.QueryBuilders;
import org.elasticsearch.search.builder.SearchSourceBuilder;
import org.joda.time.DateTime;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;

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
		manager.createAlias(EventRepository.INDEX_NAME, bucket.getId(), bucket.isVirtual() ? bucket.getAliases() : Lists.newArrayList(new Alias(bucket.getId())));
		index.store(Bucket.TYPE_NAME, bucket.getId(), bucket.toJson(), timestamp, true);
	}

	public void update(Bucket from, Bucket to, DateTime timestamp) {
		if (!Objects.equal(from.getAliases(), to.getAliases())) {
			manager.updateAlias(EventRepository.INDEX_NAME, from.getId(), to.getAliases());
		}
		index.update(Bucket.TYPE_NAME, to.getId(), to.toJson(), timestamp, true);
	}

	public boolean delete(String bucketId) {
		Preconditions.checkState(!isAliased(bucketId), "Can't delete an aliased bucket");
		boolean deleted = index.delete(Bucket.TYPE_NAME, bucketId, true);
		if (deleted) {
			manager.deleteAlias(EventRepository.INDEX_NAME, bucketId);
		}
		return deleted;
	}

	/**
	 * Returns <code>true</code> if a bucket is aliased from another bucket.
	 */
	public boolean isAliased(String bucketId) {
		return !index.find(QueryBuilders.termQuery(Bucket.ALIASES + ".@id", bucketId)).isEmpty();
	}

	public Bucket find(String bucketId) {
		ObjectNode node = index.get(Bucket.TYPE_NAME, bucketId);
		return node != null ? toObject(node) : null;
	}

	public PartialList<Bucket> find(int offset, int limit) {
		return find(new BucketQuery(), BucketQuery.DEFAULT_ORDER, offset, limit);
	}

	public PartialList<Bucket> find(BucketQuery query, SearchOrder order, int offset, int limit) {
		SearchSourceBuilder search = new SearchSourceBuilder()
			.query(query.build()).version(true).from(offset).size(limit);
		order.apply(search);
		return new BucketList(index.find(search));
	}

	public void find(BucketQuery query, Callback<Bucket> callback) {
		super.find(query.build(), callback);
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
