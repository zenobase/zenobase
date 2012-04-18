package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;

import com.google.inject.Inject;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.schema.ObjectField;
import com.zenobase.services.BucketManager;

public class UpdateBucketCommand extends CommandSupport {

	private static final String TYPE = "update bucket";
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateBucketCommand(ObjectNode node) {
		super(node);
	}

	public UpdateBucketCommand(Identity principal, Bucket from, Bucket to) {
		super(TYPE, principal);
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
	public Command reverse(Identity principal) {
		return new UpdateBucketCommand(principal, getTo(), getFrom());
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
		public Command parse(ObjectNode node) {
			return new UpdateBucketCommand(node);
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
