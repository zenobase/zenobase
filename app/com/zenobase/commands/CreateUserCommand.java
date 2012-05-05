package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class CreateUserCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create user", 1);
	private static final ObjectField USER = new ObjectField("user");

	public CreateUserCommand(ObjectNode node) {
		super(node);
	}

	public CreateUserCommand(Identity principal, User user) {
		super(TYPE, principal);
		setParameter(USER, user.toJson());
	}

	public User getUser() {
		return new User(getParameter(USER));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteUserCommand(principal, getUser());
	}

	@Override
	public String toString() {
		return String.format("signed up as %s", getUser().getName());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new CreateUserCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<CreateUserCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(CreateUserCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateUserCommand command) {
			repository.store(command.getUser());
		}
	}
}
