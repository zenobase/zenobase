package commands;

import models.Bucket;
import models.Event;
import secure.Identity;

public class CreateEventCommand extends CommandSupport {

	private final Bucket bucket;
	private final Event event;

	public CreateEventCommand(Bucket bucket, Identity identity, Event event) {
		super(identity);
		this.bucket = bucket;
		this.event = event;
	}

	public void execute() {
		bucket.add(event);
	}

	public Command reverse() {
		return new DeleteEventCommand(bucket, getIdentity(), event);
	}

	@Override
	public String toString() {
		return String.format("added an event to '%s'", bucket);
	}
}
