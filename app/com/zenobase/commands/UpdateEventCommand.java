package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.elasticsearch.index.engine.VersionConflictEngineException;
import play.Logger;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class UpdateEventCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("update event", 3);
	private static final TokenField BUCKET = new TokenField("bucket");
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateEventCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public UpdateEventCommand(Identity principal, String bucketId, Event from, Event to) {
		super(TYPE, principal);
		setParameter(BUCKET, bucketId);
		setParameter(FROM, from.toJson());
		setParameter(TO, to.toJson());
		addCost(1);
	}

	private String getBucketId() {
		return getParameter(BUCKET);
	}

	private Event getFrom() {
		return new Event(getParameter(FROM));
	}

	private Event getTo() {
		return new Event(getParameter(TO));
	}

	@Override
	public Command reverse(Identity principal) {
		Event from = getTo();
		Event to = getFrom();
		from.setVersion(from.getVersion() + 1);
		to.setVersion(to.getVersion() + 1);
		return new UpdateEventCommand(principal, getBucketId(), from, to);
	}

	@Override
	public String toString() {
		return String.format("updated event %s in %s", getTo().getId(), getBucketId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			UpdateEventCommand command = new UpdateEventCommand(node);
			switch (version) {
				case 3: return command;
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<UpdateEventCommand> {

		private final EventRepository repository;

		@Inject
		public Handler(EventRepository repository) {
			super(UpdateEventCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateEventCommand command) {
			try {
				update(command);
			} catch (VersionConflictEngineException e) {
				if (e.getCurrentVersion() == -1) {
					Logger.info("Recovering from a missing event...");
					create(command);
				} else {
					command.getFrom().setVersion(command.getFrom().getVersion() - 2); // if an update was reversed, we could be two versions ahead
					command.getTo().setVersion(command.getTo().getVersion() - 2);
					Logger.info("Recovering from a version conflict...");
					update(command);
				}
			}
		}

		private void update(UpdateEventCommand command) {
			repository.update(command.getBucketId(), command.getTo().copy(), command.getTimestamp()); // copy to prevent the version number from being incremented
		}

		private void create(UpdateEventCommand command) {
			repository.add(command.getBucketId(), command.getTo().copy(), command.getTimestamp());
		}
	}
}
