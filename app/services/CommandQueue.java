package services;

import java.util.LinkedHashMap;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import commands.Command;

public class CommandQueue {

	private final LinkedHashMap<String, Command> history = Maps.newLinkedHashMap();

	public String execute(Command command) {
		command.execute();
		history.put(command.getId(), command);
		return command.getId();
	}

	public Command find(String id) {
		return history.get(id);
	}

	public ImmutableList<Command> getHistory(int offset, int limit) {
		ImmutableList<Command> commands = ImmutableList.copyOf(history.values());
		int from = Math.min(offset, size());
		int to = Math.min(offset + limit, size());
		return commands.reverse().subList(from, to);
	}

	public int size() {
		return history.size();
	}
}
