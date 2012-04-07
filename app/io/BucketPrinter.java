package io;

import models.Bucket;
import models.Permission;

import org.elasticsearch.common.base.Joiner;

import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;

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
