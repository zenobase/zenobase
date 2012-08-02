package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import play.Logger;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.services.BucketRepository;

public class UpdateBucketCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("update bucket", 2);
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
		Bucket from = getTo();
		Bucket to = getFrom();
		from.setVersion(from.getVersion() + 1);
		to.setVersion(to.getVersion() + 1);
		return new UpdateBucketCommand(principal, from, to);
	}

	@Override
	public String toString() {
		return String.format("updated '%s'", getTo());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			Logger.info("Parsing update command (v=" + version + ")");
			switch (version) {
				case 1:
					UpdateBucketCommand command = new UpdateBucketCommand(node);
					command.getTo().setVersion(command.getFrom().getVersion());
					command.setType(TYPE);
					Logger.info("Migrated update command: " + command.toJson());
					return command;
				case 2:
					return new UpdateBucketCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<UpdateBucketCommand> {

		private final BucketRepository repository;

		@Inject
		public Handler(BucketRepository repository) {
			super(UpdateBucketCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateBucketCommand command) {
			repository.update(command.getTo().copy()); // copy to prevent the version number from being incremented
		}
	}
}
