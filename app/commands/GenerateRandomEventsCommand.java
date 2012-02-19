package commands;

import secure.Identity;
import models.Bucket;

public class GenerateRandomEventsCommand extends CompoundCommand {

	public GenerateRandomEventsCommand(Identity identity, Bucket bucket, int events) {
		super(identity);
		RandomEvent rand = new RandomEvent(bucket.getId());
		for (int i = 0; i < events; ++i) {
			add(new CreateEventCommand(bucket, rand.next()));
		}
	}

	@Override
	public String toString() {
		return String.format("added %,d random events", size());
	}
}
