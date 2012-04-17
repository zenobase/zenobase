package commands;

public abstract class CommandParserSupport implements CommandParser {

	private CommandParserRegistry registry;

	@Override
	public void registered(CommandParserRegistry registry) {
		this.registry = registry;
	}

	protected CommandParserRegistry getRegistry() {
		return registry;
	}
}
