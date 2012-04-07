package commands;

import models.Event;
import secure.Identity;
import services.BucketManager;

public class CreateEventCommand extends CommandSupport {

	private final BucketManager bucket;
	private final Event event;

	public CreateEventCommand(BucketManager bucket, Identity identity, Event event) {
		super(identity);
		this.bucket = bucket;
		this.event = event;
	}

	@Override
	public void execute() {
		bucket.add(event.getBucket(), event);
	}

	@Override
	public Command reverse(Identity identity) {
		return new DeleteEventCommand(bucket, identity, event);
	}

	@Override
	public String toString() {
		return String.format("added an event to '%s'", bucket);
	}
}
