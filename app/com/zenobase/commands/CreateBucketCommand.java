package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class CreateBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create bucket", 4);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private CreateBucketCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateBucketCommand(Identity principal, Bucket bucket) {
		super(TYPE, principal);
		setParameter(BUCKET, bucket.toJson());
	}

	public Bucket getBucket() {
		return new Bucket(getParameter(BUCKET));
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
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			CreateBucketCommand command = new CreateBucketCommand(node);
			command.setType(TYPE);
			switch (version) {
				case 4: return command;
			}
			return null;
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
			repository.store(command.getBucket(), command.getTimestamp());
		}
	}
}
