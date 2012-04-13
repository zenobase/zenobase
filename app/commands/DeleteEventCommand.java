package commands;

import models.Event;
import models.Identity;

public class DeleteEventCommand extends CommandSupport {

	public static final String TYPE = "delete event";

	private final String bucketId;
	private final Event event;

	public DeleteEventCommand(Identity identity, String bucketId, Event event) {
		super(TYPE, identity);
		this.bucketId = bucketId;
		this.event = event;
	}

	public String getBucketId() {
		return bucketId;
	}

	public Event getEvent() {
		return event;
	}

	@Override
	public Command reverse(Identity identity) {
		return new CreateEventCommand(identity, bucketId, event);
	}

	@Override
	public String toString() {
		return String.format("removed an event from '%s'", bucketId);
	}
}
