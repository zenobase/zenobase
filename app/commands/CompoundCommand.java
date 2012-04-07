package commands;

import java.util.List;

import secure.Identity;

import com.google.common.collect.Lists;

public class CompoundCommand extends CommandSupport {

	private final List<Command> commands = Lists.newArrayList();
	private final String message, reverseMessage;

	public CompoundCommand(Identity identity, String message, String reverseMessage) {
		super(identity);
		this.message = message;
		this.reverseMessage = reverseMessage;
	}

	public void add(Command command) {
		commands.add(command);
	}

	@Override
	public void execute() {
		for (Command command : commands) {
			command.execute();
		}
	}

	@Override
	public Command reverse(Identity identity) {
		CompoundCommand reverse = new CompoundCommand(identity, reverseMessage, message);
		for (Command command : Lists.reverse(commands)) {
			reverse.add(command.reverse(identity));
		}
		return reverse;
	}

	@Override
	public String toString() {
		return message;
	}
}
