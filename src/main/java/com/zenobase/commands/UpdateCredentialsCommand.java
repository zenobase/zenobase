package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.JsonPatch;
import com.zenobase.models.Identity;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.tasks.Credentials;

public class UpdateCredentialsCommand extends UpdateCommandSupport {

	private static final Command.Type TYPE = new Command.Type("update credentials", 1);

	private UpdateCredentialsCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	protected UpdateCredentialsCommand(Identity principal, String credentialsId, ObjectNode from, ObjectNode to) {
		super(TYPE, principal, credentialsId, from, to);
	}

	@Override
	protected Command newInstance(Identity principal, String objectId, ObjectNode from, ObjectNode to) {
		return new UpdateCredentialsCommand(principal, objectId, from, to);
	}

	public Credentials apply(Credentials credentials) {
		return new Credentials(new JsonPatch(Objects.requireNonNull(getFrom()), Objects.requireNonNull(getTo()))
				.apply(credentials.toJson()));
	}

	@Override
	public String toString() {
		return String.format("updated credentials %s", getObjectId());
	}

	public static Builder builder(Credentials credentials) {
		return new Builder() {
			@Override
			public UpdateCredentialsCommand build() {
				return new UpdateCredentialsCommand(
						credentials.getPrincipal(), credentials.getId(), getFrom(), getTo());
			}
		};
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new UpdateCredentialsCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<UpdateCredentialsCommand> {

		private final CredentialsRepository repository;

		@Inject
		public Handler(CredentialsRepository repository) {
			super(UpdateCredentialsCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateCredentialsCommand command) {
			Credentials credentials = repository.find(Objects.requireNonNull(command.getObjectId()));
			Preconditions.checkNotNull(credentials, "Can't find credentials: %s", command.getObjectId());
			repository.update(command.apply(credentials));
		}
	}
}
