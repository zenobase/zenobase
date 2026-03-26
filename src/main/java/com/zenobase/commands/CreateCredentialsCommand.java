package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;

public class CreateCredentialsCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create credentials", 1);
	private static final ObjectField CREDENTIALS = new ObjectField("credentials");

	private CreateCredentialsCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateCredentialsCommand(Identity principal, Credentials credentials) {
		super(TYPE, principal, credentials.getCreated());
		setParameter(CREDENTIALS, credentials.toJson());
	}

	public Credentials getCredentials() {
		return new Credentials(getParameter(CREDENTIALS));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteCredentialsCommand(principal, getCredentials());
	}

	@Override
	public String toString() {
		Credentials credentials = getCredentials();
		return String.format("created %s credentials %s", credentials.getType(), credentials.getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new CreateCredentialsCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<CreateCredentialsCommand> {

		private final CredentialsRepository repository;

		@Inject
		public Handler(CredentialsRepository repository) {
			super(CreateCredentialsCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateCredentialsCommand command) {
			repository.store(command.getCredentials(), command.getTimestamp());
		}
	}
}
