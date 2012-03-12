package commands;

import java.util.List;

import secure.Identity;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class CompoundCommand extends CommandSupport {

	private final List<Command> commands = Lists.newArrayList();

	public CompoundCommand(Identity identity) {
		super(identity);
	}

	public void add(Command command) {
		commands.add(command);
	}

	@Override
	public void execute() {
		for (Command command : commands) {
			command.execute(); // TODO: undo all if one fails
		}
	}

	@Override
	public Command reverse() {
		CompoundCommand reverse = new CompoundCommand(getIdentity());
		for (Command command : Lists.reverse(commands)) {
			reverse.add(command.reverse());
		}
		return reverse;
	}

	public int size() {
		return commands.size();
	}

	@Override
	public String toString() {
		return String.format("%s x %d", Iterables.getLast(commands), commands.size());
	}
}
