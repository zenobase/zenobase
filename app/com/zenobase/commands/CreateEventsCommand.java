package com.zenobase.commands;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

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
			addParameter(EVENTS, event.toJson());
		}
		addCost(events.size());
	}

	public String getBucketId() {
		return getParameter(BUCKET_ID);
	}

	public List<Event> getEvents() {
		List<ObjectNode> nodes = getParameters(EVENTS);
		List<Event> events = Lists.newArrayListWithCapacity(nodes.size());
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
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			CreateEventsCommand command = new CreateEventsCommand(node);
			switch (version) {
				case 1: return command;
			}
			return null;
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
			repository.add(command.getBucketId(), command.getEvents(), command.getTimestamp());
		}
	}
}
