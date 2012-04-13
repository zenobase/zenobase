package commands;

public abstract class CommandHandlerSupport<C extends Command> implements CommandHandler<C> {

	private final Class<C> type;

	protected CommandHandlerSupport(Class<C> type) {
		this.type = type;
	}

	@Override
	public Class<C> getType() {
		return type;
	}

	@Override
	public void executeCommand(Command command) {
		execute(type.cast(command));
	}

	protected abstract void execute(C command);
}
