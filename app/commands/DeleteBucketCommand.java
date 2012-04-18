package commands;

import models.Bucket;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.BucketManager;

import com.google.inject.Inject;

public class DeleteBucketCommand extends CommandSupport {

	private static final String TYPE = "delete bucket";
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private DeleteBucketCommand(ObjectNode object) {
		super(object);
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
		return String.format("deleted bucket '%s'", getBucket());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new DeleteBucketCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<DeleteBucketCommand> {

		private final BucketManager manager;

		@Inject
		public Handler(BucketManager manager) {
			super(DeleteBucketCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(DeleteBucketCommand command) {
			manager.deleteBucket(command.getBucket().getId());
		}
	}
}
