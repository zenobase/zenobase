package com.zenobase.commands;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import jakarta.inject.Inject;
import org.jspecify.annotations.Nullable;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.tasks.Task;

public class CreateTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create task", 2);
	private static final ObjectField TASK = new ObjectField("task");

	private CreateTaskCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateTaskCommand(Identity principal, Task task) {
		super(TYPE, principal, task.getCreated());
		setParameter(TASK, task.toJson());
	}

	public Task getTask() {
		return new Task(Objects.requireNonNull(getParameter(TASK)));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteTaskCommand(principal, getTask());
	}

	@Override
	public String toString() {
		Task task = getTask();
		return String.format("created %s task %s", task.getType(), task.getId());
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.name();
		}

		@Override
		public @Nullable Command parse(ObjectNode node, int version) {
			return switch (version) {
				case 2 -> new CreateTaskCommand(node);
				default -> null;
			};
		}
	}

	public static class Handler extends CommandHandler<CreateTaskCommand> {

		private final TaskRepository repository;

		@Inject
		public Handler(TaskRepository repository) {
			super(CreateTaskCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(CreateTaskCommand command) {
			repository.store(command.getTask());
		}
	}
}
