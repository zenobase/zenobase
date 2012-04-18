package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.ObjectField;
import com.zenobase.services.UserManager;

public class CreateUserCommand extends CommandSupport {

	private static final String TYPE = "create user";
	private static final ObjectField USER = new ObjectField("user");

	public CreateUserCommand(ObjectNode node) {
		super(node);
	}

	public CreateUserCommand(Identity principal, User user) {
		super(TYPE, principal);
		setParameter(USER, user.toJson());
	}

	private User getUser() {
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
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode node) {
			return new CreateUserCommand(node);
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
