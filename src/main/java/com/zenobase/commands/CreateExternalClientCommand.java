package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.ObjectField;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.repositories.ExternalClientRepository;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Records that a user has been observed connecting via a given third-party client for the first time. The command's
 * timestamp is the client's {@code created}. Subsequent observations of the same {@code (user, client_id)} pair
 * do not produce additional commands (the handler is idempotent).
 */
public class CreateExternalClientCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create external client", 1);
	private static final ObjectField CLIENT = new ObjectField("client");

	private CreateExternalClientCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateExternalClientCommand(Identity principal, ExternalClient client) {
		super(TYPE, principal, client.getCreated());
		setParameter(CLIENT, client.toJson());
	}

	public ExternalClient getClient() {
		return new ExternalClient(Objects.requireNonNull(getParameter(CLIENT)));
	}

	@Override
	public String toString() {
		ExternalClient client = getClient();
		String name = client.getName();
		return String.format("registered external client %s", name != null ? name : client.getClient());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			var command = new CreateExternalClientCommand(node);
			command.setType(TYPE);
			return switch (version) {
				case 1 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<CreateExternalClientCommand> {

		private final ExternalClientRepository repository;

		@Inject
		public Handler(ExternalClientRepository repository) {
			super(CreateExternalClientCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateExternalClientCommand command) {
			ExternalClient incoming = command.getClient();
			ExternalClient existing = repository.find(incoming.getId());
			if (existing == null) {
				repository.store(incoming);
			}
			// idempotent: a second creation command for the same (user, client) is a no-op
		}
	}
}
