package commands;

import models.Identity;
import models.User;

public class CreateUserCommand extends CommandSupport {

	public static final String TYPE = "create user";

	private final User user;

	public CreateUserCommand(Identity identity, User user) {
		super(TYPE, identity);
		this.user = user;
	}

	public User getUser() {
		return user;
	}

	@Override
	public Command reverse(Identity identity) {
		return new DeleteUserCommand(identity, user);
	}

	@Override
	public String toString() {
		return String.format("signed up as %s", user.getName());
	}
}
