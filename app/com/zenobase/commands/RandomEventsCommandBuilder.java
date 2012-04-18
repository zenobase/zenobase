package com.zenobase.commands;

import com.zenobase.models.Identity;

public class RandomEventsCommandBuilder {

	private final Identity principal;
	private final String bucketId;

	public RandomEventsCommandBuilder(Identity principal, String bucketId) {
		this.principal = principal;
		this.bucketId = bucketId;
	}

	public Command build(int events) {
		CompoundCommand command = new CompoundCommand(principal, 
			String.format("added %,d random events", events), 
			String.format("removed %,d random events", events));
		RandomEvent rand = new RandomEvent(bucketId, principal);
		for (int i = 0; i < events; ++i) {
			command.add(new CreateEventCommand(principal, bucketId, rand.next()));
		}
		return command;
	}
}
