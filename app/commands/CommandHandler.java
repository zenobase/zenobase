package commands;

public interface CommandHandler<C extends Command> {
	
	Class<C> getType();

	void executeCommand(Command command);
}
