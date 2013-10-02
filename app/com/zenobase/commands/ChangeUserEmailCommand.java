package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class ChangeUserEmailCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("change user email", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final TokenField FROM = new TokenField("from");
	private static final TokenField TO = new TokenField("to");
	private static final BooleanField FROM_VERIFIED = new BooleanField("fromVerified");
	private static final BooleanField TO_VERIFIED = new BooleanField("toVerified");

	private ChangeUserEmailCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public ChangeUserEmailCommand(Identity principal, String username, String from, String to, boolean fromVerified, boolean toVerified) {
		super(TYPE, principal);
		setParameter(USERNAME, username);
		setParameter(FROM, from);
		setParameter(TO, to);
		setParameter(FROM_VERIFIED, fromVerified);
		setParameter(TO_VERIFIED, toVerified);
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

	private Boolean getFromVerified() {
		return getParameter(FROM_VERIFIED);
	}

	private Boolean getToVerified() {
		return getParameter(TO_VERIFIED);
	}

	@Override
	public Command reverse(Identity principal) {
		return new ChangeUserEmailCommand(principal, getUsername(), getTo(), getFrom(), getToVerified(), getFromVerified());
	}

	@Override
	public String toString() {
		return String.format("changed email for user %s", getUsername());
	}

	public static class Parser extends CommandParser {

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

	public static class Handler extends CommandHandler<ChangeUserEmailCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(ChangeUserEmailCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(ChangeUserEmailCommand command) {
			User user = repository.find(command.getUsername());
			user.setEmail(command.getTo());
			user.setVerified(command.getToVerified());
			repository.update(user, command.getTimestamp());
		}
	}
}
