package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.jspecify.annotations.Nullable;

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
		return Objects.requireNonNull(getParameter(USERNAME));
	}

	private boolean isSuspend() {
		return Objects.requireNonNull(getParameter(SUSPEND));
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
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new SuspendUserCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<SuspendUserCommand> {

		private final UserRepository repository;

		public Handler(UserRepository repository) {
			super(SuspendUserCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(SuspendUserCommand command) {
			User user = repository.find(command.getName());
			if (user != null) {
				user.setSuspended(command.isSuspend());
				repository.update(user, command.getTimestamp());
			} else {
				throw new NonExistentUserException("Tried to suspend a nonexistent user: " + command.getName());
			}
		}
	}
}
