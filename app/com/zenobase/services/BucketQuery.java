package com.zenobase.services;

import org.apache.lucene.search.join.ScoreMode;
import org.opensearch.index.query.QueryBuilders;

import com.zenobase.json.RolesField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;

public class BucketQuery extends QuerySupport {

	public static final SearchOrder DEFAULT_ORDER = new SearchOrder(Bucket.CREATED.getName(), false);

	public BucketQuery principalEqualTo(Identity principal) {
		add(QueryBuilders.nestedQuery(Bucket.ROLES.getName(),
			QueryBuilders.termQuery(Bucket.ROLES.getName() + "." + RolesField.PRINCIPAL, principal.getId()),
			ScoreMode.None));
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

	public BucketQuery isRefreshable() {
		equalTo(Bucket.REFRESH, true);
		return this;
	}

	public BucketQuery includeArchived(boolean b) {
		if (!b) {
			notEqualTo(Bucket.ARCHIVED, true);
		}
		return this;
	}

	public BucketQuery queryString(String query) {
		super.queryString(query, Bucket.ID.getName());
		return this;
	}
}
