package com.zenobase.services;

import org.opensearch.client.opensearch._types.FieldValue;
import org.opensearch.client.opensearch._types.query_dsl.ChildScoreMode;
import org.opensearch.client.opensearch._types.query_dsl.Query;

import com.zenobase.json.RolesField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;

public class BucketQuery extends QuerySupport {

	public static final SearchOrder DEFAULT_ORDER = new SearchOrder(Bucket.CREATED.getName(), false);

	public BucketQuery principalEqualTo(Identity principal) {
		add(Query.of(q -> q.nested(n -> n.path(Bucket.ROLES.getName())
				.query(Query.of(q2 -> q2.term(t -> t.field(Bucket.ROLES.getName() + "." + RolesField.PRINCIPAL)
						.value(FieldValue.of(principal.id())))))
				.scoreMode(ChildScoreMode.None))));
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
