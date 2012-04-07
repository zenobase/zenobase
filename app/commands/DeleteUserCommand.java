package commands;

import secure.Identity;
import secure.User;
import secure.UserManager;

public class DeleteUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User user;

	public DeleteUserCommand(UserManager manager, Identity identity, User user) {
		super(identity);
		this.manager = manager;
		this.user = user;
	}

	@Override
	public void execute() {
		manager.delete(user);
	}

	@Override
	public CreateUserCommand reverse(Identity identity) {
		return new CreateUserCommand(manager, identity, user);
	}

	@Override
	public String toString() {
		return String.format("deleted user %s", user.getName());
	}
}
