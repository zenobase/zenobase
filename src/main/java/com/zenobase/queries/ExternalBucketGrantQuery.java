package com.zenobase.queries;

import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.Identity;
import com.zenobase.repositories.QuerySupport;
import com.zenobase.services.SearchOrder;

public class ExternalBucketGrantQuery extends QuerySupport {

	public static final SearchOrder DEFAULT_ORDER = new SearchOrder(ExternalBucketGrant.CREATED.getName(), false);

	public ExternalBucketGrantQuery userEqualTo(Identity user) {
		equalTo(ExternalBucketGrant.USER, user.id());
		return this;
	}

	public ExternalBucketGrantQuery clientEqualTo(Identity client) {
		equalTo(ExternalBucketGrant.CLIENT, client.id());
		return this;
	}

	public ExternalBucketGrantQuery bucketEqualTo(String bucketId) {
		equalTo(ExternalBucketGrant.BUCKET, bucketId);
		return this;
	}
}
