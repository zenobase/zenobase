package services;

import commands.Command;
import common.Callback;
import common.PartialList;

public interface CommandStore {

	void put(Command command);

	Command find(String id);

	void findAll(Callback<Command> callback);

	PartialList<Command> getHistory(int offset, int limit);

	long size();
}
