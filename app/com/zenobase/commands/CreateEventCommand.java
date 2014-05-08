package com.zenobase.commands;

import javax.inject.Inject;

import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.services.EventRepository;

public class CreateEventCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create event", 4);
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
		addCost(1);
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

	public static void fix(JsonNode node) {
		if (node.isArray()) {
			for (JsonNode sourceNode : node) {
				fix(sourceNode);
			}
		} else if (node.isObject()) {
			if ("SleepCloud".equals(node.path("title")) && !"http://sleep-cloud.appspot.com/".equals(node.path("url"))) {
				Logger.warn("Correcting source url: <" + node.path("url") + ">");
				((ObjectNode) node).put("url", "http://sleep-cloud.appspot.com/");
			}
		} else if (!node.isMissingNode()) {
			throw new IllegalArgumentException("Can't handle: " + node);
		}
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			fix(node.path("parameters").path("event").path("source"));
			CreateEventCommand command = new CreateEventCommand(node);
			switch (version) {
				case 4: return command;
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
			repository.add(command.getBucketId(), command.getEvent(), command.getTimestamp());
		}
	}
}
