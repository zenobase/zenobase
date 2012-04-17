package services;

import java.util.LinkedHashMap;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.primitives.Ints;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Maps;
import com.google.inject.Inject;
import commands.Command;
import commands.CommandParserRegistry;

public class MockCommandStore implements CommandStore {

	private final CommandParserRegistry parsers;
	private final LinkedHashMap<String, ObjectNode> history = Maps.newLinkedHashMap();

	@Inject
	public MockCommandStore(CommandParserRegistry parsers) {
		this.parsers = parsers;
	}

	@Override
	public void put(Command command) {
		history.put(command.getId(), command.toJson());
	}

	@Override
	public Command find(String id) {
		ObjectNode commandNode = history.get(id);
		return commandNode != null ? parsers.parse(commandNode) : null;
	}

	@Override
	public ImmutableList<Command> getHistory(int offset, int limit) {
		ImmutableList.Builder<Command> commands = ImmutableList.builder();
		int from = Ints.checkedCast(Math.min(offset, size()));
		int to = Ints.checkedCast(Math.min(offset + limit, size()));
		for (int i = to - 1; i >= from; --i) {
			commands.add(parsers.parse(Iterables.get(history.values(), i)));
		}
		return commands.build();
	}

	@Override
	public long size() {
		return history.size();
	}
}
