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
 * Removes the {@code external_clients} row for a {@code (user, client_id)} pair. The full snapshot of the row is
 * carried in the command so the journal can be rebuilt and so {@link #reverse(Identity)} can reconstruct the row.
 */
public class DeleteExternalClientCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete external client", 1);
	private static final ObjectField CLIENT = new ObjectField("client");

	private DeleteExternalClientCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteExternalClientCommand(Identity principal, ExternalClient client) {
		super(TYPE, principal);
		setParameter(CLIENT, client.toJson());
	}

	public ExternalClient getClient() {
		return new ExternalClient(Objects.requireNonNull(getParameter(CLIENT)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateExternalClientCommand(principal, getClient());
	}

	@Override
	public String toString() {
		ExternalClient client = getClient();
		String name = client.getName();
		return String.format("revoked external client %s", name != null ? name : client.getClient());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new DeleteExternalClientCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<DeleteExternalClientCommand> {

		private final ExternalClientRepository repository;

		@Inject
		public Handler(ExternalClientRepository repository) {
			super(DeleteExternalClientCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteExternalClientCommand command) {
			ExternalClient client = command.getClient();
			repository.delete(client.getUser(), client.getClient());
			// idempotent: deleting an already-missing row is a no-op (the underlying repository.delete returns false
			// but we don't treat that as fatal — the journal can replay against a fresh index)
		}
	}
}
