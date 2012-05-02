package com.zenobase.services;

import play.Logger;
import play.Logger.ALogger;
import com.google.inject.Inject;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CompoundCommand;

public class CommandQueue {

	private final ALogger log = Logger.of("queue");
	private final CommandHandlerRegistry handlers;
	private final CommandStore store;

	@Inject
	public CommandQueue(CommandHandlerRegistry handlers, CommandStore store) {
		this.handlers = handlers;
		this.store = store;
	}

	public String dispatch(Command command) {
		log.info(String.format("%s %s", command.getPrincipal(), command.toString()));
		if (command instanceof CompoundCommand) {
			dispatch((CompoundCommand) command);
		}
		else {
			handlers.execute(command);
		}
		store.put(command);
		return command.getId();
	}

	private void dispatch(CompoundCommand command) {
		for (Command c : command.getCommands()) {
			handlers.execute(c);
		}
	}
}
