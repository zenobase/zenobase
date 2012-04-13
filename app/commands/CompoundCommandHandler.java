package commands;

import java.util.Set;

import com.google.inject.Inject;
import com.google.inject.Provider;

public class CompoundCommandHandler extends CommandHandlerSupport<CompoundCommand> {

	private final Provider<Set<CommandHandler<?>>> provider;

	@Inject
	public CompoundCommandHandler(Provider<Set<CommandHandler<?>>> provider) {
		super(CompoundCommand.class);
		this.provider = provider;
	}

	@Override
	public void execute(CompoundCommand command) {
		C: for (Command c : command.getCommands()) {
			for (CommandHandler<?> handler : provider.get()) {
				if (handler.getType().equals(c.getClass())) {
					handler.executeCommand(c);
					continue C;
				}
			}
			throw new UnsupportedOperationException("Missing handler for " + c.getClass());
		}
	}
}
