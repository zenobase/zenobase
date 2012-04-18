package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;

import com.google.inject.Inject;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.schema.BooleanField;
import com.zenobase.schema.TokenField;
import com.zenobase.services.UserManager;

public class SuspendUserCommand extends CommandSupport {

	private static final String TYPE = "suspend user";
	private static final TokenField NAME = new TokenField("name");
	private static final BooleanField SUSPEND = new BooleanField("suspend");

	private SuspendUserCommand(ObjectNode node) {
		super(node);
	}

	public SuspendUserCommand(Identity principal, String name, boolean suspend) {
		super(TYPE, principal);
		setParameter(NAME, name);
		setParameter(SUSPEND, suspend);
	}

	private String getName() {
		return getParameter(NAME);
	}

	private boolean isSuspend() {
		return getParameter(SUSPEND);
	}

	@Override
	public Command reverse(Identity principal) {
		return new SuspendUserCommand(principal, getName(), !isSuspend());
	}

	@Override
	public String toString() {
		return String.format("%s user %s", isSuspend() ? "suspended" : "unsuspended", getName());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode node) {
			return new SuspendUserCommand(node);
		}
	}

	public static class Handler extends CommandHandlerSupport<SuspendUserCommand> {

		private final UserManager manager;

		@Inject
		public Handler(UserManager manager) {
			super(SuspendUserCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(SuspendUserCommand command) {
			User user = manager.find(command.getName());
			user.setSuspended(command.isSuspend());
			manager.update(user);
		}
	}
}
