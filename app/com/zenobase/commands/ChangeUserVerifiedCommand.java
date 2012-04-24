package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.BooleanField;
import com.zenobase.schema.TokenField;
import com.zenobase.services.UserManager;

public class ChangeUserVerifiedCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("change user verified", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final BooleanField VERIFIED = new BooleanField("verified");

	private ChangeUserVerifiedCommand(ObjectNode node) {
		super(node);
	}

	public ChangeUserVerifiedCommand(Identity principal, String name, boolean verified) {
		super(TYPE, principal);
		setParameter(USERNAME, name);
		setParameter(VERIFIED, verified);
	}

	private String getName() {
		return getParameter(USERNAME);
	}

	private boolean isVerified() {
		return getParameter(VERIFIED);
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangeUserVerifiedCommand(principal, getName(), !isVerified());
	}

	@Override
	public String toString() {
		return String.format("%s user %s", isVerified() ? "verified" : "unverified", getName());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new ChangeUserVerifiedCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandlerSupport<ChangeUserVerifiedCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(ChangeUserVerifiedCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(ChangeUserVerifiedCommand command) {
			User user = manager.find(command.getName());
			user.setVerified(command.isVerified());
			manager.update(user);
		}
	}
}
