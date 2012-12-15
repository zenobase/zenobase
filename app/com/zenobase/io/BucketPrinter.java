package com.zenobase.io;

import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import com.zenobase.common.Callback;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Permission;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.BucketRepository;

public class BucketPrinter implements Callback<Bucket> {

	private final BucketRepository buckets;
	private final Chunks.Out<String> out;

	public BucketPrinter(BucketRepository buckets, Out<String> out) {
		this.out = out;
		this.buckets = buckets;
	}

	@Override
	public void call(Bucket bucket) {
		out.write(toString(bucket));
	}

	private String toString(Bucket bucket) {
		return Joiner.on('\t').join(bucket.getId(),
			Iterables.getOnlyElement(bucket.getPrincipals(Permission.ALL)),
			bucket.isPermitted(new Authorization(Identity.PUBLIC), Permission.USE) ? "published" : "unpublished",
			bucket.getCreated(), buckets.getSize(bucket.getId()), "\n");
	}
}
