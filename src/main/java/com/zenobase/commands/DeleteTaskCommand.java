package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.tasks.Task;

public class DeleteTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete task", 2);
	private static final ObjectField TASK = new ObjectField("task");

	private DeleteTaskCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public DeleteTaskCommand(Identity principal, Task task) {
		super(TYPE, principal);
		setParameter(TASK, task.toJson());
	}

	private Task getTask() {
		return new Task(Objects.requireNonNull(getParameter(TASK)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateTaskCommand(principal, getTask());
	}

	@Override
	public String toString() {
		return String.format("removed task %s", getTask().getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 2 -> new DeleteTaskCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<DeleteTaskCommand> {

		private final TaskRepository repository;

		@Inject
		public Handler(TaskRepository repository) {
			super(DeleteTaskCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(DeleteTaskCommand command) {
			repository.delete(command.getTask().getId());
		}
	}
}
