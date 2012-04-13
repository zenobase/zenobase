package commands;

import models.Event;
import models.Identity;

public class CreateEventCommand extends CommandSupport {

	public static final String TYPE = "create event";

	private final String bucketId;
	private final Event event;

	public CreateEventCommand(Identity identity, String bucketId, Event event) {
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
		return new DeleteEventCommand(identity, bucketId, event);
	}

	@Override
	public String toString() {
		return String.format("added an event to '%s'", bucketId);
	}
}
