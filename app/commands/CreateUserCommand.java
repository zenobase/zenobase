package commands;

import secure.Identity;
import secure.User;
import secure.UserManager;

public class CreateUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;

	public CreateUserCommand(UserManager manager, Identity identity, User user) {
		super(user.getIdentity());
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
