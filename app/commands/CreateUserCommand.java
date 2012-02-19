package commands;

import secure.User;
import secure.UserManager;

public class CreateUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;

	public CreateUserCommand(UserManager manager, User user) {
		super(user.getIdentity());
		this.manager = manager;
		this.user = user;
	}

	public void execute() {
		manager.store(user);
	}

	public DeleteUserCommand reverse() {
		return new DeleteUserCommand(manager, user);
	}

	@Override
	public String toString() {
		return String.format("%s signed up as %s", user.getIdentity(), user.getName());
	}
}
