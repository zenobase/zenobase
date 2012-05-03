package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class CreateUserCommand extends CommandSupport {

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

	public static class Parser extends CommandParserSupport {

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
