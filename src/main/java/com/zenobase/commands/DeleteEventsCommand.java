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
import org.jspecify.annotations.Nullable;

public class DeleteEventsCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete events", 1);
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENTS = new ObjectField("events");

	private DeleteEventsCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteEventsCommand(Identity principal, String bucketId, List<Event> events) {
		super(TYPE, principal);
		setParameter(BUCKET_ID, bucketId);
		for (Event event : events) {
			addParameter(EVENTS, event.toJson());
		}
	}

	private String getBucketId() {
		return Objects.requireNonNull(getParameter(BUCKET_ID));
	}

	private List<Event> getEvents() {
		ImmutableList<ObjectNode> nodes = getParameters(EVENTS);
		List<Event> events = new ArrayList<>(nodes.size());
		for (ObjectNode node : nodes) {
			events.add(new Event(node));
		}
		return events;
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateEventsCommand(principal, getBucketId(), getEvents());
	}

	@Override
	public String toString() {
		return String.format("removed events from %s", getBucketId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 1 -> new DeleteEventsCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<DeleteEventsCommand> {

		private final EventRepository repository;

		@Inject
		public Handler(EventRepository repository) {
			super(DeleteEventsCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteEventsCommand command) {
			List<Event> events = command.getEvents();
			List<String> ids = new ArrayList<>(events.size());
			for (Event event : events) {
				ids.add(event.getId());
			}
			repository.delete(command.getBucketId(), ids);
		}
	}
}
