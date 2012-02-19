package commands;

import models.Bucket;
import models.Event;

public class CreateEventCommand extends CommandSupport {

	private final Bucket bucket;
	private final Event event;

	public CreateEventCommand(Bucket bucket, Event event) {
		super(bucket.getIdentity());
		this.bucket = bucket;
		this.event = event;
	}

	public void execute() {
		bucket.add(event);
	}

	public Command reverse() {
		return new DeleteEventCommand(bucket, event);
	}

	@Override
	public String toString() {
		return String.format("added an event to '%s'", bucket);
	}
}
