package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.ObjectField;
import com.zenobase.services.UserManager;

public class UpdateUserCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("update user", 1);
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateUserCommand(ObjectNode node) {
		super(node);
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
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new UpdateUserCommand(node);
			}
			return null;
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
