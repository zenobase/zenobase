package commands;

import secure.User;
import secure.UserManager;

public class DeleteUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;

	public DeleteUserCommand(UserManager manager, User user) {
		super(user.getIdentity());
		this.manager = manager;
		this.user = user;
	}

	public void execute() {
		manager.delete(user);
	}

	public CreateUserCommand reverse() {
		return new CreateUserCommand(manager, user);
	}

	@Override
	public String toString() {
		return String.format("deleted user %s", user.getName());
	}
}
