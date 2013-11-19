package com.zenobase.controllers;

import com.zenobase.models.Alias;
import com.zenobase.models.Bucket;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;

class AliasValidator {

	private final BucketRepository repository;

	public AliasValidator(BucketRepository repository) {
		this.repository = repository;
	}

	public boolean checkPermissions(Bucket bucket, Authorization auth) {
		for (Alias alias : bucket.getAliases()) {
			Bucket b = repository.find(alias.getId());
			if (b == null || !b.hasRole(auth, Role.OWNER)) {
				return false;
			}
		}
		return true;
	}
}
