package services;

import java.util.LinkedHashMap;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;

import play.Logger;
import play.Logger.ALogger;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import commands.Command;
import commands.CommandParserRegistry;
import commands.CompoundCommand;

public class CommandQueue {

	private final ALogger log = Logger.of("queue");
	private final CommandHandlerRegistry handlers;
	private final CommandParserRegistry parsers;
	private final LinkedHashMap<String, ObjectNode> history = Maps.newLinkedHashMap();

	@Inject
	public CommandQueue(CommandHandlerRegistry handlers, CommandParserRegistry parsers) {
		this.handlers = handlers;
		this.parsers = parsers;
	}

	public String dispatch(Command command) {
		log.info(String.format("%s %s", command.getIdentity(), command.toString()));
		if (command instanceof CompoundCommand) {
			execute((CompoundCommand) command);
		}
		else {
			handlers.execute(command);
		}
		history.put(command.getId(), command.toJson());
		return command.getId();
	}

	private void execute(CompoundCommand command) {
		for (Command c : command.getCommands()) {
			handlers.execute(c);
		}
	}

	public Command find(String id) {
		ObjectNode commandNode = history.get(id);
		return commandNode != null ? parsers.parse(commandNode) : null;
	}

	public ImmutableList<Command> getHistory(int offset, int limit) {
		ImmutableList.Builder<Command> commands = ImmutableList.builder();
		int from = Math.min(offset, size());
		int to = Math.min(offset + limit, size());
		for (int i = to - 1; i >= from; --i) {
			commands.add(parsers.parse(Iterables.get(history.values(), i)));
		}
		return commands.build();
	}

	public int size() {
		return history.size();
	}
}
