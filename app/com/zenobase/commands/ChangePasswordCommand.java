package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.TokenField;
import com.zenobase.services.UserManager;

public class ChangePasswordCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("reset password", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField FROM = new TokenField("from");
	private static final TokenField TO = new TokenField("to");

	private ChangePasswordCommand(ObjectNode node) {
		super(node);
	}

	public ChangePasswordCommand(Identity principal, String user, String from, String to) {
		super(TYPE, principal);
		setParameter(USERNAME, user);
		setParameter(FROM, from);
		setParameter(TO, to);
	}

	private String getUser() {
		return getParameter(USERNAME);
	}

	private String getFrom() {
		return getParameter(FROM);
	}

	private String getTo() {
		return getParameter(TO);
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangePasswordCommand(principal, getUser(), getTo(), getFrom());
	}

	@Override
	public String toString() {
		return String.format("changed password for user %s", getUser());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new ChangePasswordCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandlerSupport<ChangePasswordCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(ChangePasswordCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(ChangePasswordCommand command) {
			User user = manager.find(command.getUser());
			user.setPassword(command.getTo());
			manager.update(user);
		}
	}
}
