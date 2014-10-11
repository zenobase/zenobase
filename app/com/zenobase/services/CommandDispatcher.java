package com.zenobase.services;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;
import play.Logger.ALogger;

import com.zenobase.commands.Command;
import com.zenobase.commands.CommandHandlerRegistry;
import com.zenobase.commands.CompoundCommand;

public class CommandDispatcher {

	private final ALogger log = Logger.of("dispatch");
	private final DateTime now = DateTime.now(DateTimeZone.UTC).minusSeconds(1);
	private final CommandHandlerRegistry handlers;
	private final CommandRepository repository;
	private final QuotaManager quotas;

	@Inject
	public CommandDispatcher(CommandHandlerRegistry handlers, CommandRepository repository, QuotaManager quotas) {
		this.handlers = handlers;
		this.repository = repository;
		this.quotas = quotas;
	}

	public String dispatch(Command command) {
		DateTime t = command.getTimestamp();
		log.info("[{}] {} {}", t, command.getPrincipal(), command);
		if (command.getCost() > 0 && t.isAfter(now)) { // don't spend while replaying commands
			quotas.spend(command.getPrincipal(), command.getCost());
		}
		if (command instanceof CompoundCommand) {
			dispatch((CompoundCommand) command);
		}
		else {
			handlers.execute(command);
		}
		repository.put(command);
		return command.getId();
	}

	private void dispatch(CompoundCommand command) {
		for (Command c : command.getCommands()) {
			handlers.execute(c);
		}
	}

	public void discard(Command command) {
		log.info("[{} {}]", command.getPrincipal(), command);
	}
}
