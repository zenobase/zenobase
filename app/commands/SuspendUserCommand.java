package commands;

import models.Identity;
import models.User;

public class SuspendUserCommand extends CommandSupport {

	public static final String TYPE = "suspend user";

	private final User user;
	private final boolean suspend;

	public SuspendUserCommand(Identity identity, User user, boolean suspend) {
		super(TYPE, identity);
		this.user = user;
		this.suspend = suspend;
	}

	public User getUser() {
		return user;
	}

	public boolean isSuspend() {
		return suspend;
	}

	@Override
	public Command reverse(Identity identity) {
		return new SuspendUserCommand(identity, user, !suspend);
	}

	@Override
	public String toString() {
		return String.format("%s user %s", suspend ? "suspended" : "unsuspended", user.getName());
	}
}
