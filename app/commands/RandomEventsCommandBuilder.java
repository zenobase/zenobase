package commands;

import models.Identity;

public class RandomEventsCommandBuilder {

	private final Identity identity;
	private final String bucketId;

	public RandomEventsCommandBuilder(Identity identity, String bucketId) {
		this.identity = identity;
		this.bucketId = bucketId;
	}

	public Command build(int events) {
		CompoundCommand command = new CompoundCommand(identity, 
			String.format("added %,d random events", events), 
			String.format("removed %,d random events", events));
		RandomEvent rand = new RandomEvent(bucketId, identity);
		for (int i = 0; i < events; ++i) {
			command.add(new CreateEventCommand(identity, bucketId, rand.next()));
		}
		return command;
	}
}
