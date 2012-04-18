package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;

import com.google.inject.Inject;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.ObjectField;
import com.zenobase.services.UserManager;

public class DeleteUserCommand extends CommandSupport {

	private static final String TYPE = "delete user";
	private static final ObjectField USER = new ObjectField("user");

	private DeleteUserCommand(ObjectNode node) {
		super(node);
	}

	public DeleteUserCommand(Identity principal, User user) {
		super(TYPE, principal);
		setParameter(USER, user.toJson());
	}

	private User getUser() {
		return new User(getParameter(USER));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateUserCommand(principal, getUser());
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
		public Command parse(ObjectNode node) {
			return new DeleteUserCommand(node);
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
