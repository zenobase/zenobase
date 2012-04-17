package commands;

import java.util.List;

import models.Identity;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import schema.TokenField;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

public class CompoundCommand extends CommandSupport {

	private static final String TYPE = "compound command";
	private static final TokenField MESSAGE = new TokenField("message");
	private static final TokenField UNDO_MESSAGE = new TokenField("undoMessage");
	private static final ObjectField COMMANDS = new ObjectField("commands");

	private CommandParserRegistry registry;
	private final List<Command> commands = Lists.newArrayList();

	private CompoundCommand(ObjectNode object, CommandParserRegistry registry) {
		super(object);
		this.registry = registry;
	}

	public CompoundCommand(Identity identity, String message, String undoMessage) {
		super(TYPE, identity);
		setParameter(MESSAGE, message);
		setParameter(UNDO_MESSAGE, undoMessage);
	}

	private String getMessage() {
		return getParameter(MESSAGE);
	}

	private String getUndoMessage() {
		return getParameter(UNDO_MESSAGE);
	}

	public void add(Command command) {
		commands.add(command);
		addParameter(COMMANDS, command.toJson());
	}

	public ImmutableList<Command> getCommands() {
		if (commands.isEmpty() && registry != null) {
			for (ObjectNode commandNode : getParameters(COMMANDS)) {
				commands.add(registry.parse(commandNode));
			}
		}
		return ImmutableList.copyOf(commands);
	}

	@Override
	public Command reverse(Identity identity) {
		CompoundCommand reverse = new CompoundCommand(identity, getUndoMessage(), getMessage());
		for (Command c : Lists.reverse(getCommands())) {
			reverse.add(c.reverse(identity));
		}
		return reverse;
	}

	@Override
	public String toString() {
		return getMessage();
	}

	public static class Parser extends CommandParserSupport {

		@Override
		public String getType() {
			return TYPE;
		}

		@Override
		public Command parse(ObjectNode object) {
			return new CompoundCommand(object, getRegistry());
		}
	}
}
