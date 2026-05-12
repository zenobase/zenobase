package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.ObjectField;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.Identity;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class DeleteExternalBucketGrantCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete external bucket grant", 1);
	private static final ObjectField GRANT = new ObjectField("grant");

	private DeleteExternalBucketGrantCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteExternalBucketGrantCommand(Identity principal, ExternalBucketGrant grant) {
		super(TYPE, principal);
		setParameter(GRANT, grant.toJson());
	}

	public ExternalBucketGrant getGrant() {
		return new ExternalBucketGrant(Objects.requireNonNull(getParameter(GRANT)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateExternalBucketGrantCommand(principal, getGrant());
	}

	@Override
	public String toString() {
		ExternalBucketGrant grant = getGrant();
		return String.format(
			"revoked %s access to bucket %s by client %s",
			grant.getRights(),
			grant.getBucketId(),
			grant.getClient()
		);
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			var command = new DeleteExternalBucketGrantCommand(node);
			return switch (version) {
				case 1 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<DeleteExternalBucketGrantCommand> {

		private final ExternalBucketGrantRepository repository;

		@Inject
		public Handler(ExternalBucketGrantRepository repository) {
			super(DeleteExternalBucketGrantCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteExternalBucketGrantCommand command) {
			repository.delete(command.getGrant().getId());
		}
	}
}
