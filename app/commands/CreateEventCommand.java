package commands;

import models.Event;
import models.Identity;
import services.BucketManager;

public class CreateEventCommand extends CommandSupport {

	private final BucketManager bucketManager;
	private final String bucketId;
	private final Event event;

	public CreateEventCommand(BucketManager bucketManager, Identity identity, String bucketId, Event event) {
		super(identity);
		this.bucketManager = bucketManager;
		this.bucketId = bucketId;
		this.event = event;
	}

	@Override
	public void execute() {
		bucketManager.add(bucketId, event);
	}

	@Override
	public Command reverse(Identity identity) {
		return new DeleteEventCommand(bucketManager, identity, bucketId, event);
	}

	@Override
	public String toString() {
		return String.format("added an event to '%s'", bucketManager);
	}
}
