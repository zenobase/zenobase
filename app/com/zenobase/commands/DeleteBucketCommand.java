package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketManager;

public class DeleteBucketCommand extends CommandSupport {

	private static final Command.Type TYPE = new Command.Type("delete bucket", 1);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private DeleteBucketCommand(ObjectNode node) {
		super(node);
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
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new DeleteBucketCommand(node);
			}
			return null;
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
