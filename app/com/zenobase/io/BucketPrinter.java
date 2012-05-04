package com.zenobase.io;

import org.elasticsearch.common.collect.Iterables;
import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;
import com.google.common.base.Joiner;

import com.zenobase.common.Callback;
import com.zenobase.models.Bucket;
import com.zenobase.models.Permission;

public class BucketPrinter implements Callback<Bucket> {

	private final Chunks.Out<String> out;

	public BucketPrinter(Out<String> out) {
		this.out = out;
	}

	@Override
	public void call(Bucket bucket) {
		out.write(toString(bucket));
	}

	private String toString(Bucket bucket) {
		return Joiner.on('\t').join(bucket.getId(), bucket.toString(), Iterables.getOnlyElement(bucket.getPrincipals(Permission.ALL)), "\n");
	}
}
