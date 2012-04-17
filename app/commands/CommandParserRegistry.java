package commands;

import java.util.Map;
import java.util.Set;

import org.codehaus.jackson.node.ObjectNode;

import com.google.common.base.Preconditions;
import com.google.common.collect.Maps;
import com.google.inject.Inject;

public class CommandParserRegistry {

	private final Map<String, CommandParser> parsers = Maps.newHashMap();

	@Inject
	public CommandParserRegistry(Set<CommandParser> parsers) {
		for (CommandParser parser : parsers) {
			this.parsers.put(parser.getType(), parser);		
			parser.registered(this);
		}
	}

	public Command parse(ObjectNode object) {
		String type = CommandSupport.TYPE.getValue(object);
		Preconditions.checkNotNull(type, "Missing type field in " + object);
		CommandParser parser = parsers.get(type);
		Preconditions.checkNotNull(type, "Missing parser for type " + type);
		return parser.parse(object);
	}	
}
