package commands;

import models.Bucket;
import secure.Identity;

public class GenerateRandomEventsCommand extends CompoundCommand {

	public GenerateRandomEventsCommand(Identity identity, Bucket bucket, int events) {
		super(identity);
		RandomEvent rand = new RandomEvent(bucket, identity);
		for (int i = 0; i < events; ++i) {
			add(new CreateEventCommand(bucket, identity, rand.next()));
		}
	}

	@Override
	public String toString() {
		return String.format("added %,d random events", size());
	}
}
