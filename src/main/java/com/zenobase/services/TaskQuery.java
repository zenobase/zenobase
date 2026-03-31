package com.zenobase.services;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;

public class TaskQuery extends QuerySupport {

	public TaskQuery principalEqualTo(Identity principal) {
		equalTo(Task.PRINCIPAL, principal.id());
		return this;
	}

	public TaskQuery bucketEqualTo(String bucketId) {
		equalTo(Task.BUCKET, bucketId);
		return this;
	}

	public TaskQuery queryString(String query) {
		super.queryString(query, Task.ID.getName());
		return this;
	}

	public static SearchOrder orderByCreated(boolean asc) {
		return new SearchOrder(Task.CREATED.getName(), asc);
	}
}
