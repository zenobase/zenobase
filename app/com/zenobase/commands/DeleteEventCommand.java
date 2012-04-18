package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;

import com.google.inject.Inject;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.schema.ObjectField;
import com.zenobase.schema.TokenField;
import com.zenobase.services.BucketManager;

public class DeleteEventCommand extends CommandSupport {

	private static final String TYPE = "delete event";
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENT = new ObjectField("event");

	private DeleteEventCommand(ObjectNode node) {
		super(node);
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
		return String.format("removed an event from '%s'", getBucketId());
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode node) {
			return new DeleteEventCommand(node);
		}
	}

	public static class Handler extends CommandHandlerSupport<DeleteEventCommand> {

		private final BucketManager bucketManager;

		@Inject
		public Handler(BucketManager bucketManager) {
			super(DeleteEventCommand.class);
			this.bucketManager = bucketManager;
		}

		@Override
		public void executeTyped(DeleteEventCommand command) {
			bucketManager.delete(command.getBucketId(), command.getEvent().getId());
		}
	}
}
