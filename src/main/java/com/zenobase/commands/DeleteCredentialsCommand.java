package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

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
		return new Credentials(Objects.requireNonNull(getParameter(OBJECT)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateCredentialsCommand(principal, getCredentials());
	}

	@Override
	public String toString() {
		return String.format("removed credentials %s", getCredentials().getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new DeleteCredentialsCommand(node);
				default -> null;
			};
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
