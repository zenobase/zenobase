package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class DeleteTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete task", 1);
	private static final ObjectField TASK = new ObjectField("task");

	private DeleteTaskCommand(ObjectNode node) {
		super(node);
	}

	public DeleteTaskCommand(Identity principal, Task task) {
		super(TYPE, principal);
		setParameter(TASK, task.toJson());
	}

	private Task getTask() {
		return new Task(getParameter(TASK));
	}

	@Override
	public Command reverse(Identity principal) {
		return new CreateTaskCommand(principal, getTask());
	}

	@Override
	public String toString() {
		return String.format("removed a task");
	}

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 1: return new DeleteTaskCommand(node);
			}
			return null;
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
