package com.zenobase.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;
import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.Query;
import org.opensearch.client.opensearch._types.query_dsl.TermQuery;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.joda.time.DateTime;
import play.Logger;

import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;
import com.zenobase.json.DomainNode;
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
			index.putMapping(Bucket.SCHEMA);
		}
	}

	public void store(Bucket bucket, DateTime timestamp) {
		realias(bucket);
		index.store(bucket.getId(), bucket.toJson(), timestamp, true);
	}

	public void realias(Bucket bucket) {
		manager.createAlias(EventRepository.INDEX_NAME, bucket.getId(), bucket.isVirtual() ? bucket.getAliases() : Lists.newArrayList(new Alias(bucket.getId())));
	}

	public void update(Bucket from, Bucket to, DateTime timestamp) {
		if (!Objects.equal(from.getAliases(), to.getAliases())) {
			manager.updateAlias(EventRepository.INDEX_NAME, from.getId(), to.getAliases());
		}
		DomainNode.SEQ_NO.setValue(to.toJson(), DomainNode.SEQ_NO.getValue(from.toJson()));
		DomainNode.PRIMARY_TERM.setValue(to.toJson(), DomainNode.PRIMARY_TERM.getValue(from.toJson()));
		index.update(to.getId(), to.toJson(), timestamp, true);
	}

	public boolean delete(String bucketId) {
		Preconditions.checkState(!isAliased(bucketId), "Can't delete an aliased bucket");
		boolean deleted = index.delete(bucketId, true);
		if (deleted) {
			manager.deleteAlias(EventRepository.INDEX_NAME, bucketId);
		}
		return deleted;
	}

	/**
	 * Returns <code>true</code> if a bucket is aliased from another bucket.
	 */
	public boolean isAliased(String bucketId) {
		return !index.find(Query.of(q -> q.term(t -> t.field(Bucket.ALIASES + ".@id").value(FieldValue.of(bucketId))))).isEmpty();
	}

	public Bucket find(String bucketId) {
		ObjectNode node = index.get(bucketId);
		return node != null ? toObject(node) : null;
	}

	public PartialList<Bucket> find(int offset, int limit) {
		return find(new BucketQuery(), BucketQuery.DEFAULT_ORDER, offset, limit);
	}

	public PartialList<Bucket> find(BucketQuery query, SearchOrder order, int offset, int limit) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
			.index(index.getIndexName())
			.query(query.build()).version(true).seqNoPrimaryTerm(true).from(offset).size(limit)
			.trackTotalHits(t -> t.enabled(true));
		order.apply(builder);
		return new BucketList(index.find(builder.build()));
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
