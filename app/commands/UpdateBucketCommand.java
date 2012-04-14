package commands;

import models.Bucket;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import services.BucketManager;

import com.google.inject.Inject;

public class UpdateBucketCommand extends CommandSupport {

	private static final String TYPE = "update bucket";
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateBucketCommand(ObjectNode object) {
		super(object);
	}

	public UpdateBucketCommand(Identity identity, Bucket from, Bucket to) {
		super(TYPE, identity);
		setParameter(FROM, from.toJson());
		setParameter(TO, to.toJson());
	}

	private Bucket getFrom() {
		return new Bucket(getParameter(FROM));
	}

	private Bucket getTo() {
		return new Bucket(getParameter(TO));
	}

	@Override
	public Command reverse(Identity identity) {
		return new UpdateBucketCommand(identity, getTo(), getFrom());
	}

	@Override
	public String toString() {
		return String.format("updated '%s'", getTo());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new UpdateBucketCommand(object);
		}
	}

	public static class Handler extends CommandHandlerSupport<UpdateBucketCommand> {

		private final BucketManager manager;

		@Inject
		public Handler(BucketManager manager) {
			super(UpdateBucketCommand.class);
			this.manager = manager;
		}

		@Override
		public void executeTyped(UpdateBucketCommand command) {
			manager.update(command.getTo());
		}
	}
}
