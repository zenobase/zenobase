package com.zenobase.commands;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class DeleteEventCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete event", 2);
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENT = new ObjectField("event");

	private DeleteEventCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteEventCommand(Identity principal, String bucketId, Event event) {
		super(TYPE, principal);
		setParameter(BUCKET_ID, bucketId);
		setParameter(EVENT, event.toJson());
	}

	private String getBucketId() {
		return getParameter(BUCKET_ID);
	}

	private Event getEvent() {
		return new Event(getParameter(EVENT));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateEventCommand(principal, getBucketId(), getEvent());
	}

	@Override
	public String toString() {
		return String.format("removed an event from %s", getBucketId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 2: return new DeleteEventCommand(node);
			}
			return null;
		}
	}

	public static class Handler extends CommandHandler<DeleteEventCommand> {

		private final EventRepository repository;

		@Inject
		public Handler(EventRepository repository) {
			super(DeleteEventCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteEventCommand command) {
			repository.delete(command.getBucketId(), command.getEvent().getId());
		}
	}
}
