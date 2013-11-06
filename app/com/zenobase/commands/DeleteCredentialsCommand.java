package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;

public class DeleteCredentialsCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete credentials", 1);
	private static final ObjectField OBJECT = new ObjectField("object");

	private DeleteCredentialsCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteCredentialsCommand(Identity principal, Credentials credentials) {
		super(TYPE, principal);
		setParameter(OBJECT, credentials.toJson());
	}

	private Credentials getCredentials() {
		return new Credentials(getParameter(OBJECT));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateCredentialsCommand(principal, getCredentials());
	}

	@Override
	public String toString() {
		return String.format("removed credentials");
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new DeleteCredentialsCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<DeleteCredentialsCommand> {

		private final CredentialsRepository repository;

		@Inject
		public Handler(CredentialsRepository repository) {
			super(DeleteCredentialsCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteCredentialsCommand command) {
			repository.delete(command.getCredentials().getId());
		}
	}
}
