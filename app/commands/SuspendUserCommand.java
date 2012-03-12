package commands;

import secure.User;
import secure.UserManager;

public class SuspendUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;
	private final boolean suspend;

	public SuspendUserCommand(UserManager manager, User user, boolean suspend) {
		super(user.getIdentity());
		this.manager = manager;
		this.user = user;
		this.suspend = suspend;
	}

	public void execute() {
		user.setSuspended(suspend);
		manager.store(user);
	}

	public SuspendUserCommand reverse() {
		return new SuspendUserCommand(manager, user, !suspend);
	}

	@Override
	public String toString() {
		return String.format("%s user %s", suspend ? "suspended" : "unsuspended", user.getName());
	}
}
