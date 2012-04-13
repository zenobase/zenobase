package services;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

import org.elasticsearch.common.base.Objects;

import play.Logger;
import play.Logger.ALogger;

import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import commands.Command;
import commands.CommandHandler;

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
		CommandHandler<?> handler = Objects.firstNonNull(handlers.get(command.getClass()), handlers.get(command.getClass().getSuperclass()));
		Preconditions.checkNotNull(handler, "Missing handler for %s", command.getClass());
		handler.executeCommand(command);
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
