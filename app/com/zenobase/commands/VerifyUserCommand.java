package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.BooleanField;
import com.zenobase.schema.TokenField;
import com.zenobase.services.UserManager;

public class VerifyUserCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("verify user", 1);
	private static final TokenField NAME = new TokenField("name");
	private static final BooleanField VERIFIED = new BooleanField("verified");

	private VerifyUserCommand(ObjectNode node) {
		super(node);
	}

	public VerifyUserCommand(Identity principal, String name, boolean verified) {
		super(TYPE, principal);
		setParameter(NAME, name);
		setParameter(VERIFIED, verified);
	}

	private String getName() {
		return getParameter(NAME);
	}

	private boolean isVerified() {
		return getParameter(VERIFIED);
	}

	@Override
	public Command reverse(Identity principal) {
		return new VerifyUserCommand(principal, getName(), !isVerified());
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
				case 1: return new VerifyUserCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandlerSupport<VerifyUserCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(VerifyUserCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(VerifyUserCommand command) {
			User user = manager.find(command.getName());
			user.setVerified(command.isVerified());
			manager.update(user);
		}
	}
}
