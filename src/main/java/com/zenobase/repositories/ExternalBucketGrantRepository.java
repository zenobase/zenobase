package com.zenobase.repositories;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.zenobase.common.PartialList;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.ExternalBucketGrantList;
import com.zenobase.models.Identity;
import com.zenobase.queries.ExternalBucketGrantQuery;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;
import org.opensearch.client.opensearch.core.SearchRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ExternalBucketGrantRepository extends RepositorySupport<ExternalBucketGrant> {

	private static final Logger logger = LoggerFactory.getLogger(ExternalBucketGrantRepository.class);

	static final String INDEX_NAME = "external_bucket_grants";

	private static final int FETCH_LIMIT = 1000;

	private final Index index;

	@Inject
	public ExternalBucketGrantRepository(IndexManager manager) {
		this.index = manager.getIndex(INDEX_NAME);
		if (!index.exists()) {
			logger.info("Creating external bucket grant index...");
			index.create(Integer.MAX_VALUE);
			index.putMapping(ExternalBucketGrant.SCHEMA);
		}
	}

	public void store(ExternalBucketGrant grant) {
		index.store(grant, true);
	}

	public boolean delete(String grantId) {
		return index.delete(grantId, true);
	}

	public @Nullable ExternalBucketGrant find(String grantId) {
		ObjectNode node = index.get(grantId);
		return node != null ? toObject(node) : null;
	}

	public @Nullable ExternalBucketGrant find(Identity user, Identity client, String bucketId) {
		return find(ExternalBucketGrant.id(user, client, bucketId));
	}

	public PartialList<ExternalBucketGrant> find(ExternalBucketGrantQuery query, int offset, int limit) {
		SearchRequest.Builder builder = new SearchRequest.Builder()
			.index(index.getIndexName())
			.query(query.build())
			.version(true)
			.seqNoPrimaryTerm(true)
			.from(offset)
			.size(limit)
			.trackTotalHits(t -> t.enabled(true));
		ExternalBucketGrantQuery.DEFAULT_ORDER.apply(builder);
		return new ExternalBucketGrantList(index.find(builder.build()));
	}

	/**
	 * Returns the set of bucket IDs the user has currently granted to the given client. Backed by a single search.
	 */
	public ImmutableSet<String> grantedBuckets(Identity user, Identity client) {
		var query = new ExternalBucketGrantQuery().userEqualTo(user).clientEqualTo(client);
		PartialList<ExternalBucketGrant> grants = find(query, 0, FETCH_LIMIT);
		ImmutableSet.Builder<String> result = ImmutableSet.builder();
		for (ExternalBucketGrant grant : grants) {
			result.add(grant.getBucketId());
		}
		return result.build();
	}

	@Override
	protected Index getIndex() {
		return index;
	}

	@Override
	protected ExternalBucketGrant toObject(ObjectNode node) {
		return new ExternalBucketGrant(node);
	}
}
