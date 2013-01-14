package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class DeleteBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete bucket", 2);
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

		private final BucketRepository buckets;

		@javax.inject.Inject
		public Parser(BucketRepository buckets) {
			this.buckets = buckets;
		}

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1:
					// TODO remove after migration
					Command c = new Command(node);
					Bucket b = new Bucket(c.getParameter(BUCKET));
					Bucket original = buckets.find(b.getId());
					Preconditions.checkNotNull(original, "Couldn't find bucket <%s>: %s", b.getId(), b.toJson());
					return new DeleteBucketCommand(c.getPrincipal(), original);
				case 2:
					return new DeleteBucketCommand(node);
			}
			return null;
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
			if (!repository.delete(command.getBucket().getId())) {
				Logger.warn("Tried to delete nonexistent bucket: " + command.getBucket().getId());
			}
		}
	}
}
