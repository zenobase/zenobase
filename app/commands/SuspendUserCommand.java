package commands;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import schema.BooleanField;
import schema.TokenField;
import services.UserManager;

import com.google.inject.Inject;

public class SuspendUserCommand extends CommandSupport {

	private static final String TYPE = "suspend user";
	private static final TokenField NAME = new TokenField("name");
	private static final BooleanField SUSPEND = new BooleanField("suspend");

	private SuspendUserCommand(ObjectNode object) {
		super(object);
	}

	public SuspendUserCommand(Identity identity, String name, boolean suspend) {
		super(TYPE, identity);
		setParameter(NAME, name);
		setParameter(SUSPEND, suspend);
	}

	private String getName() {
		return getParameter(NAME);
	}

	private boolean isSuspend() {
		return getParameter(SUSPEND);
	}

	@Override
	public Command reverse(Identity identity) {
		return new SuspendUserCommand(identity, getName(), !isSuspend());
	}

	@Override
	public String toString() {
		return String.format("%s user %s", isSuspend() ? "suspended" : "unsuspended", getName());
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
			User user = manager.find(command.getName());
			user.setSuspended(command.isSuspend());
			manager.update(user);
		}
	}
}
