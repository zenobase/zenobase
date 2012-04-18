package commands;

import org.codehaus.jackson.node.ObjectNode;

public interface CommandParser {

	String getType();

	Command parse(ObjectNode node);

	void registered(CommandParserRegistry registry);
}
