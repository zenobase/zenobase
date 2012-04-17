package services;

import com.google.common.collect.ImmutableList;
import commands.Command;

public interface CommandStore {

	void put(Command command);

	Command find(String id);

	ImmutableList<Command> getHistory(int offset, int limit);

	long size();

}