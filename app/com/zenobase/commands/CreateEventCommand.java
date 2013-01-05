package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class CreateEventCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create event", 1);
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENT = new ObjectField("event");

	private CreateEventCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateEventCommand(Identity principal, String bucketId, Event event) {
		super(TYPE, principal);
		setParameter(BUCKET_ID, bucketId);
		setParameter(EVENT, event.toJson());
	}

	public String getBucketId() {
		return getParameter(BUCKET_ID);
	}

	public Event getEvent() {
		return new Event(getParameter(EVENT));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteEventCommand(principal, getBucketId(), getEvent());
	}

	@Override
	public String toString() {
		return String.format("added an event to %s", getBucketId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new CreateEventCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<CreateEventCommand> {

		private final EventRepository repository;

		@Inject
		public Handler(EventRepository repository) {
			super(CreateEventCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateEventCommand command) {
			repository.add(command.getBucketId(), command.getEvent());
		}
	}
}
