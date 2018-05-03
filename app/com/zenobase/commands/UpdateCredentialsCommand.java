package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.json.JsonPatch;
import com.zenobase.models.Identity;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.tasks.Credentials;

import javax.inject.Inject;

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
		return new Credentials(new JsonPatch(getFrom(), getTo()).apply(credentials.toJson()));
	}

	@Override
	public String toString() {
		return String.format("updated credentials %s", getObjectId());
	}

	public static Builder builder(final Credentials credentials) {
		return new Builder() {
			@Override
			public UpdateCredentialsCommand build() {
				return new UpdateCredentialsCommand(credentials.getPrincipal(), credentials.getId(), getFrom(), getTo());
			}
		};
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new UpdateCredentialsCommand(node);
			}
			return null;
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
			Credentials credentials = repository.find(command.getObjectId());
			Preconditions.checkNotNull(credentials, "Can't find credentials: %s", command.getObjectId());
			repository.update(command.apply(credentials), command.getTimestamp());
		}
	}
}
