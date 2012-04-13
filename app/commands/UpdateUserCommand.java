package commands;

import models.Identity;
import models.User;

public class UpdateUserCommand extends CommandSupport {

	public static final String TYPE = "update user";

	private final User from, to;

	public UpdateUserCommand(Identity identity, User user, String email, String password, boolean verified) {
		super(TYPE, identity);
		from = user;
		to = user.copy();
		if (email != null) {
			to.setEmail(email);
			to.setVerified(verified);
		}
		if (password != null) {
			to.changePassword(password);
		}
	}

	public UpdateUserCommand(Identity identity, User from, User to) {
		super(TYPE, identity);
		this.from = from;
		this.to = to;
	}

	public User getFrom() {
		return from;
	}

	public User getTo() {
		return to;
	}

	@Override
	public Command reverse(Identity identity) {
		return new UpdateUserCommand(identity, to, from);
	}

	@Override
	public String toString() {
		return String.format("updated user %s", from.getName());
	}
}
