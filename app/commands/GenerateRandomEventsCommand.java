package commands;

import models.Identity;

public class GenerateRandomEventsCommand extends CompoundCommand {

	public GenerateRandomEventsCommand(Identity identity, String bucketId, int events) {
		super(identity, String.format("added %,d random events", events), String.format("removed %,d random events", events));
		RandomEvent rand = new RandomEvent(bucketId, identity);
		for (int i = 0; i < events; ++i) {
			add(new CreateEventCommand(identity, bucketId, rand.next()));
		}
	}
}
