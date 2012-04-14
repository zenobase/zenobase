package commands;

import models.Bucket;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.BucketManager;

import com.google.inject.Inject;

public class CreateBucketCommand extends CommandSupport {

	private static final String TYPE = "create bucket";
	private static final ObjectField BUCKET = new ObjectField("bucket");
	
	private CreateBucketCommand(ObjectNode object) {
		super(object);
	}

	public CreateBucketCommand(Identity identity, Bucket bucket) {
		super(TYPE, identity);
		setParameter(BUCKET, bucket.toJson());
	}

	private Bucket getBucket() {
		return new Bucket(getParameter(BUCKET));
	}

	@Override
	public Command reverse(Identity identity) {
		return new DeleteBucketCommand(identity, getBucket());
	}

	@Override
	public String toString() {
		return String.format("create '%s'", getBucket());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new CreateBucketCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<CreateBucketCommand> {

		private final BucketManager manager;

		@Inject
		public Handler(BucketManager manager) {
			super(CreateBucketCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(CreateBucketCommand command) {
			manager.store(command.getBucket(), true);
		}
	}
}
