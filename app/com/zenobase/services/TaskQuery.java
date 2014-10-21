package com.zenobase.services;

import org.elasticsearch.index.query.QueryBuilders;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class TaskQuery extends QuerySupport {

	public TaskQuery principalEqualTo(Identity principal) {
		equalTo(Task.PRINCIPAL, principal.getId());
		return this;
	}

	public TaskQuery bucketEqualTo(String bucketId) {
		equalTo(Task.BUCKET, bucketId);
		return this;
	}

	public TaskQuery match(String query) {
		add(QueryBuilders.queryString(query));
		return this;
	}

	public static SearchOrder orderByCreated(boolean asc) {
		return new SearchOrder(Task.CREATED.getName(), asc);
	}
}
