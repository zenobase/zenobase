package commands;

import models.Identity;
import models.User;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.UserManager;

import com.google.inject.Inject;

public class DeleteUserCommand extends CommandSupport {

	private static final String TYPE = "delete user";
	private static final ObjectField USER = new ObjectField("user");

	private DeleteUserCommand(ObjectNode object) {
		super(object);
	}

	public DeleteUserCommand(Identity identity, User user) {
		super(TYPE, identity);
		setParameter(USER, user.toJson());
	}

	private User getUser() {
		return new User(getParameter(USER));
	}

	@Override
	public Command reverse(Identity identity) {
		return new CreateUserCommand(identity, getUser());
	}

	@Override
	public String toString() {
		return String.format("deleted user %s", getUser().getName());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new DeleteUserCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<DeleteUserCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(DeleteUserCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(DeleteUserCommand command) {
			manager.delete(command.getUser());
		}
	}
}
