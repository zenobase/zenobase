package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class CreateEventCommand extends Command {

	static final Command.Type TYPE = new Command.Type("create event", 4);
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENT = new ObjectField("event");

	CreateEventCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateEventCommand(Identity principal, String bucketId, Event event) {
		super(TYPE, principal);
		setParameter(BUCKET_ID, bucketId);
		setParameter(EVENT, event.toJson());
		addCost(1);
	}

	public String getBucketId() {
		return Objects.requireNonNull(getParameter(BUCKET_ID));
	}

	public Event getEvent() {
		return new Event(Objects.requireNonNull(getParameter(EVENT)));
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
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			var command = new CreateEventCommand(node);
			return switch (version) {
				case 4 -> command;
				default -> null;
			};
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
