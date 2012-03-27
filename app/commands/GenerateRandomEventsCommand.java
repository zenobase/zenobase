package commands;

import models.Bucket;
import secure.Identity;

public class GenerateRandomEventsCommand extends CompoundCommand {

	public GenerateRandomEventsCommand(Identity identity, Bucket bucket, int events) {
		super(identity, String.format("added %,d random events", events), String.format("removed %,d random events", events));
		RandomEvent rand = new RandomEvent(bucket, identity);
		for (int i = 0; i < events; ++i) {
			add(new CreateEventCommand(bucket, identity, rand.next()));
		}
	}
}
