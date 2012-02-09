package commands;
import java.util.Locale;

import models.Bucket;


public class GenerateRandomEventsCommand extends CompoundCommand {

	public GenerateRandomEventsCommand(String user, Bucket bucket, int events) {
		super(user);
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
