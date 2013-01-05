package com.zenobase.commands;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class CompoundCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("compound command", 1);
	private static final TokenField MESSAGE = new TokenField("message");
	private static final TokenField UNDO_MESSAGE = new TokenField("undoMessage");
	private static final ObjectField COMMANDS = new ObjectField("commands");

	private CommandParserRegistry registry;
	private final List<Command> commands = Lists.newArrayList();

	CompoundCommand(ObjectNode node, CommandParserRegistry registry) {
		super(node);
		checkType(TYPE);
		this.registry = registry;
	}

	public CompoundCommand(Identity principal, String message, String undoMessage) {
		super(TYPE, principal);
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
	public CompoundCommand reverse(Identity principal) {
		CompoundCommand reverse = new CompoundCommand(principal, getUndoMessage(), getMessage());
		for (Command c : Lists.reverse(getCommands())) {
			reverse.add(c.reverse(principal));
		}
		return reverse;
	}

	@Override
	public String toString() {
		return getMessage();
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new CompoundCommand(node, getRegistry());
			}
			return null;
		}
	}
}
