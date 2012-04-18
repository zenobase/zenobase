package services;

import com.google.common.collect.ImmutableList;
import commands.Command;
import common.Callback;

public interface CommandStore {

	void put(Command command);

	Command find(String id);

	void findAll(Callback<Command> callback);

	ImmutableList<Command> getHistory(int offset, int limit);

	long size();

}