package commands;

import models.Bucket;
import secure.Identity;
import secure.User;
import secure.UserManager;
import services.BucketManager;

public class CloseAccountCommand extends CompoundCommand {

	private final User user;

	public CloseAccountCommand(Identity identity, BucketManager buckets, UserManager users, User user) {
		super(identity);
		this.user = user;
		add(new SuspendUserCommand(users, identity, user, true));
		for (Bucket bucket : buckets.findBuckets(user.getIdentity())) {
			add(new DeleteBucketCommand(buckets, getIdentity(), bucket));
		}
	}

	@Override
	public String toString() {
		return String.format("closed account of %s", user.getName());
	}
}
