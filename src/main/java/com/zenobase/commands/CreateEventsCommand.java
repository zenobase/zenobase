package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.repositories.EventRepository;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class CreateEventsCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create events", 1);
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENTS = new ObjectField("events");

	private CreateEventsCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateEventsCommand(Identity principal, String bucketId, List<Event> events) {
		this(principal, bucketId, events, DateTime.now(DateTimeZone.UTC));
	}

	public CreateEventsCommand(Identity principal, String bucketId, List<Event> events, DateTime timestamp) {
		super(TYPE, principal, timestamp);
		setParameter(BUCKET_ID, bucketId);
		for (Event event : events) {
			event.setValue(Event.AUTHOR, principal);
			addParameter(EVENTS, event.toJson());
		}
		addCost(events.size());
	}

	public String getBucketId() {
		return Objects.requireNonNull(getParameter(BUCKET_ID));
	}

	public List<Event> getEvents() {
		ImmutableList<ObjectNode> nodes = getParameters(EVENTS);
		List<Event> events = new ArrayList<>(nodes.size());
		for (ObjectNode node : nodes) {
			events.add(new Event(node));
		}
		return events;
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteEventsCommand(principal, getBucketId(), getEvents());
	}

	@Override
	public String toString() {
		return String.format("added events to %s", getBucketId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			var command = new CreateEventsCommand(node);
			return switch (version) {
				case 1 -> command;
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<CreateEventsCommand> {

		private final EventRepository repository;

		@Inject
		public Handler(EventRepository repository) {
			super(CreateEventsCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateEventsCommand command) {
			repository.add(command.getBucketId(), command.getEvents());
		}
	}
}
