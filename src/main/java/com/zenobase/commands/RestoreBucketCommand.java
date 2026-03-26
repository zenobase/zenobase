package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class RestoreBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("restore bucket", 1);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private RestoreBucketCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public RestoreBucketCommand(Identity principal, Bucket bucket) {
		super(TYPE, principal);
		setParameter(BUCKET, bucket.toJson());
	}

	private Bucket getBucket() {
		return new Bucket(Objects.requireNonNull(getParameter(BUCKET)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteBucketCommand(principal, getBucket());
	}

	@Override
	public String toString() {
		return String.format("restored bucket %s", getBucket().getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new RestoreBucketCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<RestoreBucketCommand> {

		private final BucketRepository repository;

		@Inject
		public Handler(BucketRepository repository) {
			super(RestoreBucketCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(RestoreBucketCommand command) {
			repository.store(command.getBucket(), command.getTimestamp());
		}
	}
}
