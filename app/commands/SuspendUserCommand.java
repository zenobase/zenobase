package commands;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import schema.BooleanField;
import schema.IdentityField;
import services.UserManager;

import com.google.inject.Inject;

public class SuspendUserCommand extends CommandSupport {

	private static final String TYPE = "suspend user";
	private static final IdentityField IDENTITY = new IdentityField("identity");
	private static final BooleanField SUSPEND = new BooleanField("suspend");

	private SuspendUserCommand(ObjectNode object) {
		super(object);
	}

	public SuspendUserCommand(Identity identity, Identity user, boolean suspend) {
		super(TYPE, identity);
		setParameter(IDENTITY, user);
		setParameter(SUSPEND, suspend);
	}

	private Identity getUser() {
		return getParameter(IDENTITY);
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
		return String.format("%s user %s", isSuspend() ? "suspended" : "unsuspended", getUser());
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
			User user = manager.find(command.getUser());
			user.setSuspended(command.isSuspend());
			manager.update(user);
		}
	}
}
