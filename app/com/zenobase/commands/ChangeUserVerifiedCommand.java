package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.BooleanField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public class ChangeUserVerifiedCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("change user verified", 1);
	private static final TokenField USERNAME = new TokenField("username");
	private static final BooleanField VERIFIED = new BooleanField("verified");

	private ChangeUserVerifiedCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
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

	public static class Parser extends CommandParser {

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

	public static class Handler extends CommandHandler<ChangeUserVerifiedCommand> {

		private final UserRepository repository;

		@Inject
		public Handler(UserRepository repository) {
			super(ChangeUserVerifiedCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(ChangeUserVerifiedCommand command) {
			User user = repository.find(command.getName());
			user.setVerified(command.isVerified());
			repository.update(user, command.getTimestamp());
		}
	}
}
