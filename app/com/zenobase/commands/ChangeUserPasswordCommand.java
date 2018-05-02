package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class ChangeUserPasswordCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("change user password", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField FROM = new TokenField("from");
	private static final TokenField TO = new TokenField("to");

	private ChangeUserPasswordCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public ChangeUserPasswordCommand(Identity principal, String username, String from, String to) {
		super(TYPE, principal);
		setParameter(USERNAME, username);
		setParameter(FROM, from);
		setParameter(TO, to);
	}

	private String getUsername() {
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
		return new ChangeUserPasswordCommand(principal, getUsername(), getTo(), getFrom());
	}

	@Override
	public String toString() {
		return String.format("changed password for user %s", getUsername());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new ChangeUserPasswordCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<ChangeUserPasswordCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(ChangeUserPasswordCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(ChangeUserPasswordCommand command) {
			User user = repository.find(command.getUsername());
			user.setHashedPassword(command.getTo());
			repository.update(user, command.getTimestamp());
		}
	}
}
