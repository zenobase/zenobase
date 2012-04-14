package commands;

import models.Event;
import models.Identity;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import schema.TokenField;
import services.BucketManager;

import com.google.inject.Inject;

public class DeleteEventCommand extends CommandSupport {

	private static final String TYPE = "delete event";
	private static final TokenField BUCKET_ID = new TokenField("bucketId");
	private static final ObjectField EVENT = new ObjectField("event");

	private DeleteEventCommand(ObjectNode object) {
		super(object);
	}

	public DeleteEventCommand(Identity identity, String bucketId, Event event) {
		super(TYPE, identity);
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
	public Command reverse(Identity identity) {
		return new CreateEventCommand(identity, getBucketId(), getEvent());
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
		public Command parse(ObjectNode object) {
			return new DeleteEventCommand(object);
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
