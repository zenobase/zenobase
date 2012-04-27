package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserManager;

public class ChangeUserEmailCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("change user email", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final BooleanField VERIFIED = new BooleanField("verified");
	private static final TokenField FROM = new TokenField("from");
	private static final TokenField TO = new TokenField("to");

	private ChangeUserEmailCommand(ObjectNode node) {
		super(node);
	}

	public ChangeUserEmailCommand(Identity principal, String username, boolean verified, String from, String to) {
		super(TYPE, principal);
		setParameter(USERNAME, username);
		setParameter(VERIFIED, verified);
		setParameter(FROM, from);
		setParameter(TO, to);
	}

	private String getUsername() {
		return getParameter(USERNAME);
	}

	private Boolean getVerified() {
		return getParameter(VERIFIED);
	}

	private String getFrom() {
		return getParameter(FROM);
	}

	private String getTo() {
		return getParameter(TO);
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangeUserEmailCommand(principal, getUsername(), getVerified(), getTo(), getFrom());
	}

	@Override
	public String toString() {
		return String.format("changed email for %s", getUsername());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new ChangeUserEmailCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandlerSupport<ChangeUserEmailCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(ChangeUserEmailCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(ChangeUserEmailCommand command) {
			User user = manager.find(command.getUsername());
			user.setEmail(command.getTo());
			user.setVerified(command.getVerified());
			manager.update(user);
		}
	}
}
