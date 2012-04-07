package commands;

import secure.Identity;
import services.BucketManager;

public class GenerateRandomEventsCommand extends CompoundCommand {

	public GenerateRandomEventsCommand(Identity identity, BucketManager bucketManager, String bucketId, int events) {
		super(identity, String.format("added %,d random events", events), String.format("removed %,d random events", events));
		RandomEvent rand = new RandomEvent(bucketId, identity);
		for (int i = 0; i < events; ++i) {
			add(new CreateEventCommand(bucketManager, identity, bucketId, rand.next()));
		}
	}
}
