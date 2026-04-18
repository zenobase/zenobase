package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.repositories.BucketRepository;
import jakarta.inject.Inject;
import java.util.Objects;
import org.jspecify.annotations.Nullable;

public class CreateBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create bucket", 4);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private CreateBucketCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateBucketCommand(Identity principal, Bucket bucket) {
		super(TYPE, principal, Objects.requireNonNull(bucket.getCreated()));
		setParameter(BUCKET, bucket.toJson());
	}

	public Bucket getBucket() {
		return new Bucket(Objects.requireNonNull(getParameter(BUCKET)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteBucketCommand(principal, getBucket());
	}

	@Override
	public String toString() {
		return String.format("created bucket %s", getBucket().getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			var command = new CreateBucketCommand(node);
			command.setType(TYPE);
			return switch (version) {
				case 4 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<CreateBucketCommand> {

		private final BucketRepository repository;

		@Inject
		public Handler(BucketRepository repository) {
			super(CreateBucketCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateBucketCommand command) {
			repository.store(command.getBucket());
		}
	}
}
