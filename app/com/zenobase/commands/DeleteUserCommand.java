package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class DeleteUserCommand extends Command {

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

	public static class Parser extends CommandParser {

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

	public static class Handler extends CommandHandler<DeleteUserCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(DeleteUserCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteUserCommand command) {
			if (!repository.delete(command.getUser())) {
				Logger.warn("Tried to delete nonexistent user: " + command.getUser().getName());
			}
		}
	}
}
