package commands;

import models.Bucket;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.BucketManager;

import com.google.inject.Inject;

public class RestoreBucketCommand extends CommandSupport {

	private static final String TYPE = "restore bucket";
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private RestoreBucketCommand(ObjectNode node) {
		super(node);
	}

	public RestoreBucketCommand(Identity principal, Bucket bucket) {
		super(TYPE, principal);
		setParameter(BUCKET, bucket.toJson());
	}

	private Bucket getBucket() {
		return new Bucket(getParameter(BUCKET));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteBucketCommand(principal, getBucket());
	}

	@Override
	public String toString() {
		return String.format("restored bucket '%s'", getBucket());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode node) {
			return new RestoreBucketCommand(node);
		}
	}

	public static class Handler extends CommandHandlerSupport<RestoreBucketCommand> {

		private final BucketManager manager;

		@Inject
		public Handler(BucketManager manager) {
			super(RestoreBucketCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(RestoreBucketCommand command) {
			manager.store(command.getBucket(), false);
		}
	}
}
