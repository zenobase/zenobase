package commands;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.UserManager;

import com.google.inject.Inject;

public class CreateUserCommand extends CommandSupport {

	private static final String TYPE = "create user";
	private static final ObjectField USER = new ObjectField("user");

	public CreateUserCommand(ObjectNode object) {
		super(object);
	}

	public CreateUserCommand(Identity identity, User user) {
		super(TYPE, identity);
		setParameter(USER, user.toJson());
	}

	private User getUser() {
		return new User(getParameter(USER));
	}

	@Override
	public Command reverse(Identity identity) {
		return new DeleteUserCommand(identity, getUser());
	}

	@Override
	public String toString() {
		return String.format("signed up as %s", getUser().getName());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new CreateUserCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<CreateUserCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(CreateUserCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(CreateUserCommand command) {
			manager.store(command.getUser());
		}
	}
}
