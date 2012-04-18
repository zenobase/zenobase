package services;

import play.Logger;
import play.Logger.ALogger;

import com.google.inject.Inject;
import commands.Command;
import commands.CompoundCommand;
import common.Callback;

public class CommandReplay {

	private final ALogger log = Logger.of("replay");
	private final CommandHandlerRegistry handlers;
	private final CommandStore store;

	@Inject
	public CommandReplay(CommandHandlerRegistry handlers, CommandStore store) {
		this.handlers = handlers;
		this.store = store;
	}

	public void replay() {
		Logger.info("Replaying " + store.size() + " command(s)...");
		store.findAll(new Callback<Command>() {
			@Override
			public void call(Command command) {
				log.info(String.format("%s %s", command.getIdentity(), command.toString()));
				if (command instanceof CompoundCommand) {
					dispatch((CompoundCommand) command);
				}
				else {
					handlers.execute(command);
				}
			}
		});
	}

	private void dispatch(CompoundCommand command) {
		for (Command c : command.getCommands()) {
			handlers.execute(c);
		}
	}
}
