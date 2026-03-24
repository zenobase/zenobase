package com.zenobase.commands;

public abstract class CommandHandler<C extends Command> {

	private final Class<C> type;

	protected CommandHandler(Class<C> type) {
		this.type = type;
	}

	public Class<C> getType() {
		return type;
	}

	public void execute(Command command) {
		executeTyped(type.cast(command));
	}

	protected abstract void executeTyped(C command);
}
