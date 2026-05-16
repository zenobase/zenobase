package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.TokenField;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.repositories.ExternalClientRepository;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

/**
 * Replaces a third-party client's bucket grants with a snapshot of the new set. The command carries the desired state
 * rather than the diff: replaying it sets {@code readable_buckets} to the value at the time the user clicked Save.
 *
 * <p>{@code writable_buckets} will be added here as a separate parameter when write support lands; until then the
 * field is implicitly empty.
 */
public class UpdateExternalClientGrantsCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("update external client grants", 1);
	private static final IdentityField USER = new IdentityField("user");
	private static final IdentityField CLIENT = new IdentityField("client");
	private static final TokenField READABLE_BUCKETS = new TokenField("readable_buckets");

	private UpdateExternalClientGrantsCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public UpdateExternalClientGrantsCommand(
		Identity principal,
		Identity user,
		Identity client,
		List<String> readableBuckets
	) {
		super(TYPE, principal);
		setParameter(USER, user);
		setParameter(CLIENT, client);
		for (String bucketId : readableBuckets) {
			addParameter(READABLE_BUCKETS, bucketId);
		}
	}

	public Identity getUser() {
		return Objects.requireNonNull(getParameter(USER));
	}

	public Identity getClient() {
		return Objects.requireNonNull(getParameter(CLIENT));
	}

	public List<String> getReadableBuckets() {
		return getParameters(READABLE_BUCKETS);
	}

	@Override
	public String toString() {
		return String.format("set readable buckets for client %s to %s", getClient(), getReadableBuckets());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			var command = new UpdateExternalClientGrantsCommand(node);
			return switch (version) {
				case 1 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<UpdateExternalClientGrantsCommand> {

		private final ExternalClientRepository repository;

		@Inject
		public Handler(ExternalClientRepository repository) {
			super(UpdateExternalClientGrantsCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateExternalClientGrantsCommand command) {
			ExternalClient client = repository.find(ExternalClient.id(command.getUser(), command.getClient()));
			if (client == null) {
				// Updating grants before the client was registered is a no-op — replay safety.
				return;
			}
			client.setReadableBuckets(command.getReadableBuckets());
			repository.update(client);
		}
	}
}
