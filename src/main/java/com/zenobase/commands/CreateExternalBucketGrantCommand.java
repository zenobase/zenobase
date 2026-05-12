package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.ObjectField;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.Identity;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class CreateExternalBucketGrantCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create external bucket grant", 1);
	private static final ObjectField GRANT = new ObjectField("grant");

	private CreateExternalBucketGrantCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateExternalBucketGrantCommand(Identity principal, ExternalBucketGrant grant) {
		super(TYPE, principal, grant.getCreated());
		setParameter(GRANT, grant.toJson());
	}

	public ExternalBucketGrant getGrant() {
		return new ExternalBucketGrant(Objects.requireNonNull(getParameter(GRANT)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteExternalBucketGrantCommand(principal, getGrant());
	}

	@Override
	public String toString() {
		ExternalBucketGrant grant = getGrant();
		return String.format(
			"granted %s access to bucket %s by client %s",
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
			var command = new CreateExternalBucketGrantCommand(node);
			command.setType(TYPE);
			return switch (version) {
				case 1 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<CreateExternalBucketGrantCommand> {

		private final ExternalBucketGrantRepository repository;

		@Inject
		public Handler(ExternalBucketGrantRepository repository) {
			super(CreateExternalBucketGrantCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateExternalBucketGrantCommand command) {
			repository.store(command.getGrant());
		}
	}
}
