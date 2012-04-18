package commands;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.UserManager;

import com.google.inject.Inject;

public class UpdateUserCommand extends CommandSupport {

	private static final String TYPE = "update user";
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateUserCommand(ObjectNode object) {
		super(object);
	}

	public UpdateUserCommand(Identity principal, User from, User to) {
		super(TYPE, principal);
		setParameter(FROM, from.toJson());
		setParameter(TO, to.toJson());
	}

	private User getFrom() {
		return new User(getParameter(FROM));
	}

	private User getTo() {
		return new User(getParameter(TO));
	}

	@Override
	public Command reverse(Identity principal) {
		return new UpdateUserCommand(principal, getTo(), getFrom());
	}

	@Override
	public String toString() {
		return String.format("updated user %s", getTo().getName());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new UpdateUserCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<UpdateUserCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(UpdateUserCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(UpdateUserCommand command) {
			manager.update(command.getTo());
		}
	}
}
