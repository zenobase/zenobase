package com.zenobase.services;

import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.json.RolesField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;

public class BucketQuery extends QuerySupport {

	public static final SearchOrder DEFAULT_ORDER = new SearchOrder(Bucket.CREATED.getName(), false);

	public BucketQuery principalEqualTo(Identity principal) {
		add(QueryBuilders.nestedQuery(Bucket.ROLES.getName(),
			QueryBuilders.termQuery(RolesField.PRINCIPAL, principal.getId())));
		return this;
	}

	public BucketQuery isAlias(boolean b) {
		if (b) {
			notNull(Bucket.ALIASES);
		} else {
			isNull(Bucket.ALIASES);
		}
		return this;
	}
}
