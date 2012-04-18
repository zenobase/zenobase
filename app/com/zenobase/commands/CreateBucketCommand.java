package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;

import com.google.inject.Inject;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.schema.ObjectField;
import com.zenobase.services.BucketManager;

public class CreateBucketCommand extends CommandSupport {

	private static final String TYPE = "create bucket";
	private static final ObjectField BUCKET = new ObjectField("bucket");
	
	private CreateBucketCommand(ObjectNode node) {
		super(node);
	}

	public CreateBucketCommand(Identity principal, Bucket bucket) {
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
		return String.format("created '%s'", getBucket());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode node) {
			return new CreateBucketCommand(node);
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
