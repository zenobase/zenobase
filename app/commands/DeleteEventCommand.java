package commands;

import models.Event;
import secure.Identity;
import services.BucketManager;

public class DeleteEventCommand extends CommandSupport {

	private final BucketManager bucketManager;
	private final Event event;

	public DeleteEventCommand(BucketManager bucketManager, Identity identity, Event event) {
		super(identity);
		this.bucketManager = bucketManager;
		this.event = event;
	}

	public void execute() {
		bucketManager.delete(event.getBucket(), event.getId());
	}

	public Command reverse(Identity identity) {
		return new CreateEventCommand(bucketManager, identity, event);
	}

	@Override
	public String toString() {
		return String.format("removed an event from '%s'", bucketManager);
	}
}
