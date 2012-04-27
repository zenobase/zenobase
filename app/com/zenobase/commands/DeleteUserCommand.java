package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class DeleteUserCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("delete user", 1);
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
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new DeleteUserCommand(node);
			}
			return null;
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
