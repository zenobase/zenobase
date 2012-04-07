package commands;

import common.Callback;

import models.Bucket;
import secure.Identity;
import secure.User;
import secure.UserManager;
import services.BucketManager;

public class CloseAccountCommand extends CompoundCommand {

	public CloseAccountCommand(Identity identity, final BucketManager buckets, UserManager users, User user) {
		super(identity, String.format("closed account %s", user.getName()), String.format("reopened account %s", user.getName()));
		add(new SuspendUserCommand(users, identity, user, true));
		buckets.findBuckets(user.asIdentity(), new Callback<Bucket>() {
			@Override
			public void call(Bucket bucket) {
				add(new DeleteBucketCommand(buckets, getIdentity(), bucket));
			}
		});
	}
}
