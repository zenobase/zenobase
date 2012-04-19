package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.schema.ObjectField;
import com.zenobase.services.BucketManager;

public class RestoreBucketCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("restore bucket", 1);
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
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new RestoreBucketCommand(node);
			}
			return null;
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
