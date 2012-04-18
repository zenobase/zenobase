package com.zenobase.services;

import com.zenobase.commands.Command;
import com.zenobase.common.Callback;
import com.zenobase.common.PartialList;

public interface CommandStore {

	void put(Command command);

	Command find(String id);

	void findAll(Callback<Command> callback);

	PartialList<Command> getHistory(int offset, int limit);

	long size();
}
