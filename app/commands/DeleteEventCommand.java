package commands;

import models.Event;
import secure.Identity;
import services.BucketManager;

public class DeleteEventCommand extends CommandSupport {

	private final BucketManager bucketManager;
	private final String bucketId;
	private final Event event;

	public DeleteEventCommand(BucketManager bucketManager, Identity identity, String bucketId, Event event) {
		super(identity);
		this.bucketManager = bucketManager;
		this.bucketId = bucketId;
		this.event = event;
	}

	@Override
	public void execute() {
		bucketManager.delete(bucketId, event.getId());
	}

	@Override
	public Command reverse(Identity identity) {
		return new CreateEventCommand(bucketManager, identity, bucketId, event);
	}

	@Override
	public String toString() {
		return String.format("removed an event from '%s'", bucketManager);
	}
}
