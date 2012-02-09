package services;

import java.util.Collection;
import java.util.LinkedHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import commands.Command;

public class CommandQueue {

	private final LinkedHashMap<String, Command> history = Maps.newLinkedHashMap();

	public void execute(Command command) {
		command.execute();
		history.put(command.getId(), command);
	}

	public Command find(String id) {
		return history.get(id);
	}

	public ImmutableList<Command> getHistory(int n) {
		Collection<Command> commands = history.values();
		return ImmutableList.copyOf(Iterables.skip(commands, Math.max(commands.size() - n, 0))).reverse();
	}
}
