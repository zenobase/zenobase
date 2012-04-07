package commands;

import secure.Identity;
import secure.User;
import secure.UserManager;

public class SuspendUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;
	private final boolean suspend;

	public SuspendUserCommand(UserManager manager, Identity identity, User user, boolean suspend) {
		super(identity);
		this.manager = manager;
		this.user = user;
		this.suspend = suspend;
	}

	@Override
	public void execute() {
		user.setSuspended(suspend);
		manager.update(user);
	}

	@Override
	public SuspendUserCommand reverse(Identity identity) {
		return new SuspendUserCommand(manager, identity, user, !suspend);
	}

	@Override
	public String toString() {
		return String.format("%s user %s", suspend ? "suspended" : "unsuspended", user.getName());
	}
}
