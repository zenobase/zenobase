package commands;

import models.Identity;
import models.User;

public class DeleteUserCommand extends CommandSupport {

	public static final String TYPE = "delete user";

	private final User user;

	public DeleteUserCommand(Identity identity, User user) {
		super(TYPE, identity);
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	@Override
	public Command reverse(Identity identity) {
		return new CreateUserCommand(identity, user);
	}

	@Override
	public String toString() {
		return String.format("deleted user %s", user.getName());
	}
}
