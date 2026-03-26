package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class OptInCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("opt in", 1);
	private static final TokenField USERNAME = new TokenField("username");

	private OptInCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public OptInCommand(Identity principal, String name) {
		super(TYPE, principal);
		setParameter(USERNAME, name);
	}

	private String getName() {
		return getParameter(USERNAME);
	}

	@Override
	public Command reverse(Identity principal) {
		return new OptOutCommand(principal, getName());
	}

	@Override
	public String toString() {
		return String.format("opted in %s", getName());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new OptInCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<OptInCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(OptInCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(OptInCommand command) {
			User user = repository.find(command.getName());
			if (user != null) {
				user.setOptedOut(false);
				repository.update(user, command.getTimestamp());
			} else {
				throw new NonExistentUserException("Tried to opt in a nonexistent user: " + command.getName());
			}
		}
	}
}
