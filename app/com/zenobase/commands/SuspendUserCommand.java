package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class SuspendUserCommand extends Command {

	static final Command.Type TYPE = new Command.Type("suspend user", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final BooleanField SUSPEND = new BooleanField("suspend");

	private SuspendUserCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public SuspendUserCommand(Identity principal, String name, boolean suspend) {
		super(TYPE, principal);
		setParameter(USERNAME, name);
		setParameter(SUSPEND, suspend);
	}

	private String getName() {
		return getParameter(USERNAME);
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

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new SuspendUserCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<SuspendUserCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(SuspendUserCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(SuspendUserCommand command) {
			User user = repository.find(command.getName());
			user.setSuspended(command.isSuspend());
			repository.update(user, command.getTimestamp());
		}
	}
}
