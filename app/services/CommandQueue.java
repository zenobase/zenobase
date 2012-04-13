package services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import play.Logger;
import play.Logger.ALogger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import commands.Command;
import commands.CommandHandler;
import commands.CompoundCommand;

public class CommandQueue {

	private final ALogger log = Logger.of("queue");
	private final Map<Class<?>, CommandHandler<?>> handlers = Maps.newHashMap();
	private final LinkedHashMap<String, Command> history = Maps.newLinkedHashMap(); // TODO Map<String, ObjectNode>

	@Inject
	public CommandQueue(Set<CommandHandler<?>> handlers) {
		for (CommandHandler<?> handler : handlers) {
			this.handlers.put(handler.getType(), handler);		
		}
	}

	public String dispatch(Command command) {
		log.info(String.format("%s %s", command.getIdentity(), command.toString()));
		if (command instanceof CompoundCommand) {
			execute((CompoundCommand) command);
		}
		else {
			execute(command);
		}

		history.put(command.getId(), command);
		return command.getId();
	}

	private void execute(CompoundCommand command) {
		for (Command c : command.getCommands()) {
			execute(c);
		}
	}

	private void execute(Command command) {
		CommandHandler<?> handler = handlers.get(command.getClass());
		Preconditions.checkNotNull(handler, "Missing handler for %s", command.getClass());
		handler.executeCommand(command);
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
