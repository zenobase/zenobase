package com.zenobase.commands;

public interface CommandHandler<C extends Command> {
	
	Class<C> getType();

	void execute(Command command);
}
