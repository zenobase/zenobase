package com.zenobase.io;

import play.mvc.Results.Chunks;
import play.mvc.Results.Chunks.Out;
import com.google.common.base.Joiner;
import com.google.common.collect.Iterables;

import com.zenobase.common.Callback;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.services.EventRepository;

public class BucketPrinter implements Callback<Bucket> {

	private final EventRepository events;
	private final Chunks.Out<String> out;

	public BucketPrinter(EventRepository events, Out<String> out) {
		this.out = out;
		this.events = events;
	}

	@Override
	public void call(Bucket bucket) {
		out.write(toString(bucket));
	}

	private String toString(Bucket bucket) {
		return Joiner.on('\t').join(bucket.getId(),
			Iterables.getOnlyElement(bucket.getPrincipals(Role.OWNER)),
			bucket.hasRole(new Authorization(Identity.PUBLIC), Role.VIEWER) ? "published" : "unpublished",
			bucket.getCreated(), events.size(bucket.getId()), "\n");
	}
}
