package commands;

import models.User;
import models.Identity;
import services.UserManager;

public class CreateUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;

	public CreateUserCommand(UserManager manager, Identity identity, User user) {
		super(identity);
		this.manager = manager;
		this.user = user;
	}

	@Override
	public void execute() {
		manager.store(user);
	}

	@Override
	public DeleteUserCommand reverse(Identity identity) {
		return new DeleteUserCommand(manager, identity, user);
	}

	@Override
	public String toString() {
		return String.format("signed up as %s", user.getName());
	}
}
