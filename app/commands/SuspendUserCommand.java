package commands;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import schema.BooleanField;
import schema.ObjectField;
import services.UserManager;

import com.google.inject.Inject;

public class SuspendUserCommand extends CommandSupport {

	private static final String TYPE = "suspend user";
	private static final ObjectField USER = new ObjectField("user");
	private static final BooleanField SUSPEND = new BooleanField("suspend");

	private SuspendUserCommand(ObjectNode object) {
		super(object);
	}

	public SuspendUserCommand(Identity identity, User user, boolean suspend) {
		super(TYPE, identity);
		setParameter(USER, user.toJson());
		setParameter(SUSPEND, suspend);
	}

	private User getUser() {
		return new User(getParameter(USER));
	}

	private boolean isSuspend() {
		return getParameter(SUSPEND);
	}

	@Override
	public Command reverse(Identity identity) {
		return new SuspendUserCommand(identity, getUser(), !isSuspend());
	}

	@Override
	public String toString() {
		return String.format("%s user %s", isSuspend() ? "suspended" : "unsuspended", getUser().getName());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new SuspendUserCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<SuspendUserCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(SuspendUserCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(SuspendUserCommand command) {
			User user = command.getUser().copy();
			user.setSuspended(command.isSuspend());
			manager.update(user);
		}
	}
}
