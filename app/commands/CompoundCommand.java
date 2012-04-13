package commands;

import java.util.List;

import models.Identity;

import com.google.common.collect.Lists;

public class CompoundCommand extends CommandSupport {

	public static final String TYPE = "compound command";

	private final List<Command> commands = Lists.newArrayList();
	private final String message, reverseMessage;

	public CompoundCommand(Identity identity, String message, String reverseMessage) {
		super(TYPE, identity);
		this.message = message;
		this.reverseMessage = reverseMessage;
	}

	public void add(Command command) {
		commands.add(command);
	}

	public List<Command> getCommands() {
		return commands;
	}

	@Override
	public Command reverse(Identity identity) {
		CompoundCommand reverse = new CompoundCommand(identity, reverseMessage, message);
		for (Command c : Lists.reverse(commands)) {
			reverse.add(c.reverse(identity));
		}
		return reverse;
	}

	@Override
	public String toString() {
		return message;
	}
}
