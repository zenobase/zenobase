package com.zenobase.io;

import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;

import com.google.common.base.Joiner;
import com.zenobase.models.Bucket;
import com.zenobase.models.Permission;

public class BucketPrinter {

	private final Chunks.Out<String> out;

	public BucketPrinter(Out<String> out) {
		this.out = out;
	}

	public void print(Bucket bucket) {
		out.write(toString(bucket));
	}

	private String toString(Bucket bucket) {
		return Joiner.on('\t').join(bucket.getId(), bucket.toString(), bucket.getPermissions().get(Permission.ALL), "\n");
	}
}
