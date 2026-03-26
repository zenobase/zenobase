package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class DeleteBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete bucket", 3);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private DeleteBucketCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteBucketCommand(Identity principal, Bucket bucket) {
		super(TYPE, principal);
		setParameter(BUCKET, bucket.toJson());
	}

	private Bucket getBucket() {
		return new Bucket(getParameter(BUCKET));
	}

	@Override
	public Command reverse(Identity principal) {
		return new RestoreBucketCommand(principal, getBucket());
	}

	@Override
	public String toString() {
		return String.format("deleted bucket %s", getBucket().getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			var command = new DeleteBucketCommand(node);
			return switch (version) {
				case 3 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<DeleteBucketCommand> {

		private final BucketRepository repository;

		@Inject
		public Handler(BucketRepository repository) {
			super(DeleteBucketCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteBucketCommand command) {
			repository.delete(command.getBucket().getId());
		}
	}
}
