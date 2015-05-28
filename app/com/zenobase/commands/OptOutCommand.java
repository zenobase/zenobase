package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class OptOutCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("opt out", 1);
	private static final TokenField USERNAME = new TokenField("username");

	private OptOutCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public OptOutCommand(Identity principal, String name) {
		super(TYPE, principal);
		setParameter(USERNAME, name);
	}

	private String getName() {
		return getParameter(USERNAME);
	}

	@Override
	public Command reverse(Identity principal) {
		return new OptInCommand(principal, getName());
	}

	@Override
	public String toString() {
		return String.format("opted out %s", getName());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new OptOutCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<OptOutCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(OptOutCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(OptOutCommand command) {
			User user = repository.find(command.getName());
			user.setOptedOut(true);
			repository.update(user, command.getTimestamp());
		}
	}
}
