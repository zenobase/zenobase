package com.zenobase.commands;

import java.util.List;

import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;

import com.zenobase.json.ObjectField;
import com.zenobase.json.TokenField;
import com.zenobase.models.Event;
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
		addCost(command.getCost());
	}

	private void set(List<Command> commands) {
		commands.clear();
		setCost(0);
		setParameter(COMMANDS, null);
		for (Command command : commands) {
			add(command);
		}
	}

	public Command unwrap() {
		return commands.size() == 1 ? commands.get(0) : this;
	}

	public ImmutableList<Command> getCommands() {
		if (commands.isEmpty() && registry != null) {
			for (ObjectNode commandNode : getParameters(COMMANDS)) {
				Command command = registry.parse(commandNode);
				if (command instanceof CompoundCommand) {
					commands.addAll(((CompoundCommand) command).getCommands());
				} else {
					commands.add(command);
				}
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
				case 1: return migrate(new CompoundCommand(node, getRegistry()));
			}
			return null;
		}
	}

	private static Command migrate(CompoundCommand command) {
		List<Command> commands = Lists.newArrayList();
		List<Event> events = Lists.newArrayList();
		String bucketId = null;
		Command.Type type = null;
		for (Command c : command.getCommands()) {
			if (CreateEventCommand.TYPE.equals(c.getType())) {
				Preconditions.checkState(type == null || type.equals(CreateEventCommand.TYPE), "Expected additions only: %s", command);
				type = CreateEventCommand.TYPE;
				CreateEventCommand cec = new CreateEventCommand(c.toJson());
				Preconditions.checkState(bucketId == null || bucketId.equals(cec.getBucketId()), "Expected a single bucket: %s", command);
				bucketId = cec.getBucketId();
				events.add(cec.getEvent());

			} else if (DeleteEventCommand.TYPE.equals(c.getType())) {
				Preconditions.checkState(type == null || type.equals(DeleteEventCommand.TYPE), "Expected deletions only: %s", command);
				type = DeleteEventCommand.TYPE;
				DeleteEventCommand dec = new DeleteEventCommand(c.toJson());
				Preconditions.checkState(bucketId == null || bucketId.equals(dec.getBucketId()), "Expected a single bucket: %s", command);
				bucketId = dec.getBucketId();
				events.add(dec.getEvent());
			} else {
				Preconditions.checkState(type == null, "Expected no commands after additions and deletions: %s", command);
				commands.add(c);
			}
		}
		if (CreateEventCommand.TYPE.equals(type)) {
			Preconditions.checkNotNull(bucketId, "Expected a bucket: %s", command);
			Preconditions.checkState(!events.isEmpty(), "Expected one or more events: %s", command);
			commands.add(new CreateEventsCommand(command.getPrincipal(), bucketId, events, command.getTimestamp()));
		} else if (DeleteEventCommand.TYPE.equals(type)) {
			Preconditions.checkNotNull(bucketId, "Expected a bucket: %s", command);
			Preconditions.checkState(!events.isEmpty(), "Expected one or more events: %s", command);
			commands.add(new DeleteEventsCommand(command.getPrincipal(), bucketId, events, command.getTimestamp()));
		}
		if (!events.isEmpty()) {
			Logger.info("Migrated!");
			if (events.size() == 7) {
				Logger.info("From: {}", command);
			}
			command.set(commands);
			if (events.size() == 7) {
				Logger.info("To: {}", command.unwrap());
			}
		}
		return command.unwrap();
	}
}
