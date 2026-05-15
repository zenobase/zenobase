package com.zenobase.queries;

import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.repositories.QuerySupport;
import com.zenobase.services.SearchOrder;

public class ExternalClientQuery extends QuerySupport {

	public static final SearchOrder DEFAULT_ORDER = new SearchOrder(ExternalClient.CREATED.getName(), false);

	public ExternalClientQuery userEqualTo(Identity user) {
		equalTo(ExternalClient.USER, user.id());
		return this;
	}
}
