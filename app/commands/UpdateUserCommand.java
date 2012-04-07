package commands;

import models.User;
import models.Identity;
import services.UserManager;

public class UpdateUserCommand extends CommandSupport {

	private final UserManager manager;
	private final User oldUser, newUser;

	public UpdateUserCommand(UserManager manager, Identity identity, User user, String email, String password, boolean verified) {
		super(identity);
		this.manager = manager;
		oldUser = user;
		newUser = user.copy();
		if (email != null) {
			newUser.setEmail(email);
			newUser.setVerified(verified);
		}
		if (password != null) {
			newUser.changePassword(password);
		}
	}

	@Override
	public void execute() {
		manager.update(newUser);
	}

	@Override
	public UpdateUserCommand reverse(Identity identity) {
		return new UpdateUserCommand(manager, identity, newUser, oldUser.getEmail(), oldUser.getPassword(), oldUser.isVerified());
	}

	@Override
	public String toString() {
		return String.format("updated user %s", oldUser.getName());
	}
}
