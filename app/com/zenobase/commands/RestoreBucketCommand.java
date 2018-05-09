package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class RestoreBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("restore bucket", 1);
	private static final ObjectField BUCKET = new ObjectField("bucket");

	private RestoreBucketCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
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
		return String.format("restored bucket %s", getBucket().getId());
	}

	public static class Parser extends CommandParser {

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

	public static class Handler extends CommandHandler<RestoreBucketCommand> {

		private final BucketRepository repository;

		@Inject
		public Handler(BucketRepository repository) {
			super(RestoreBucketCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(RestoreBucketCommand command) {
			repository.store(command.getBucket(), command.getTimestamp());
		}
	}
}
