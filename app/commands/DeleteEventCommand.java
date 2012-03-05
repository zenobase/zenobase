package commands;

import models.Bucket;
import models.Event;
import secure.Identity;

public class DeleteEventCommand extends CommandSupport {

	private final Bucket bucket;
	private final Event event;

	public DeleteEventCommand(Bucket bucket, Identity identity, Event event) {
		super(identity);
		this.bucket = bucket;
		this.event = event;
	}

	public void execute() {
		bucket.delete(event.getId());
	}

	public Command reverse() {
		return new CreateEventCommand(bucket, getIdentity(), event);
	}

	@Override
	public String toString() {
		return String.format("removed an event from '%s'", bucket);
	}
}
