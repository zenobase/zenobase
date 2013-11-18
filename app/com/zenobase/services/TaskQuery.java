package com.zenobase.services;

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
}
