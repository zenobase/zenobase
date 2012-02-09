package commands;

import java.util.List;

import com.google.common.base.Preconditions;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

public class CompoundCommand extends CommandSupport {

	private final List<Command> commands = Lists.newArrayList();

	public CompoundCommand(String user) {
		super(user);
	}

	public void add(Command command) {
		if (!commands.isEmpty()) {
			Preconditions.checkArgument(Iterables.getLast(commands).getClass().equals(command.getClass()));
		}
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
		CompoundCommand reverse = new CompoundCommand(getUser());
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
		return String.format("%s × %d", Iterables.getLast(commands), commands.size());
	}
}
