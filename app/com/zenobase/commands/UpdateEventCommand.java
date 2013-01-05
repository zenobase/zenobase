package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class UpdateEventCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("update event", 1);
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
			switch (version) {
				case 1:
					return new UpdateEventCommand(node);
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
			repository.update(command.getBucketId(), command.getTo().copy()); // copy to prevent the version number from being incremented
		}
	}
}
