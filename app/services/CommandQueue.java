package services;

import java.util.LinkedHashMap;

import play.Logger;
import play.Logger.ALogger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import commands.Command;

public class CommandQueue {

	private final ALogger log = Logger.of("queue");
	private final LinkedHashMap<String, Command> history = Maps.newLinkedHashMap();

	public String execute(Command command) {
		log.info(String.format("%s %s", command.getIdentity(), command.toString()));
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
