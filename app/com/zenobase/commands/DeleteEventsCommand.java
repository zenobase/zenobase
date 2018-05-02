package com.zenobase.commands;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

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
		return getParameter(BUCKET_ID);
	}

	private List<Event> getEvents() {
		List<ObjectNode> nodes = getParameters(EVENTS);
		List<Event> events = Lists.newArrayListWithCapacity(nodes.size());
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
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new DeleteEventsCommand(node);
			}
			return null;
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
			List<String> ids = Lists.newArrayListWithCapacity(events.size());
			for (Event event : events) {
				ids.add(event.getId());
			}
			repository.delete(command.getBucketId(), ids);
		}
	}
}
