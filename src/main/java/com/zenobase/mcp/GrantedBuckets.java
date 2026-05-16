package com.zenobase.mcp;

import com.google.common.collect.ImmutableSet;
import com.zenobase.common.PartialList;
import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Shared pipeline behind {@code resources/list} ({@link com.zenobase.mcp.resources.BucketResourceProvider}) and the
 * {@code buckets} tool ({@link com.zenobase.mcp.tools.BucketsTool}): resolves the external client's grants, filters
 * the user's buckets to the granted set, and surfaces a consent-URL hint when the result would be empty so the model
 * can guide the user back to Settings.
 */
public class GrantedBuckets {

	private static final int LIST_LIMIT = 500;

	private final BucketRepository buckets;
	private final ExternalClientRepository clients;
	private final ConsentEnforcer enforcer;

	@Inject
	public GrantedBuckets(BucketRepository buckets, ExternalClientRepository clients, ConsentEnforcer enforcer) {
		this.buckets = buckets;
		this.clients = clients;
		this.enforcer = enforcer;
	}

	public Result list(Authorization auth) {
		Identity clientId = auth.getClient();
		if (clientId == null) {
			return new Result(List.of(), enforcer.consentUrl());
		}
		ExternalClient client = clients.find(auth.getPrincipal(), clientId);
		ImmutableSet<String> readable =
			client != null ? ImmutableSet.copyOf(client.getReadableBuckets()) : ImmutableSet.of();
		PartialList<Bucket> userBuckets = buckets.find(
			new BucketQuery().principalEqualTo(auth.getPrincipal()).includeArchived(true),
			BucketQuery.DEFAULT_ORDER,
			0,
			LIST_LIMIT
		);
		List<Bucket> filtered = new ArrayList<>();
		for (Bucket bucket : userBuckets) {
			if (readable.contains(bucket.getId())) {
				filtered.add(bucket);
			}
		}
		return new Result(filtered, filtered.isEmpty() ? enforcer.consentUrl() : null);
	}

	public record Result(List<Bucket> buckets, @Nullable String consentUrl) {}
}
