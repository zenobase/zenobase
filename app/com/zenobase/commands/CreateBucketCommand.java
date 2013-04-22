package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.migrate.Migrate21to22;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class CreateBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create bucket", 3);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private CreateBucketCommand(ObjectNode node) {
		super(node);
		// TODO checkType(TYPE);
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
			switch (version) {
				case 2:
					return new CreateBucketCommand(command.getPrincipal(), Migrate21to22.migrate(command.getBucket()));
				case 3:
					return command;
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
			repository.store(command.getBucket(), command.getTimestamp(), true);
		}
	}
}
