package commands;

import models.Bucket;
import models.Identity;
import models.User;
import services.BucketManager;

import common.Callback;

public class CloseAccountCommandBuilder {

	private final Identity principal;
	private final BucketManager buckets;
	private final User user;

	public CloseAccountCommandBuilder(Identity principal, BucketManager buckets, User user) {
		this.principal = principal;
		this.buckets = buckets;
		this.user = user;
	}

	public Command build() {
		final CompoundCommand command = new CompoundCommand(principal, String.format("closed account %s", user.getName()), String.format("reopened account %s", user.getName()));
		command.add(new SuspendUserCommand(principal, user.getName(), true));
		buckets.findBuckets(user.asIdentity(), new Callback<Bucket>() {
			@Override
			public void call(Bucket bucket) {
				command.add(new DeleteBucketCommand(principal, bucket));
			}
		});
		return command;
	}
}
