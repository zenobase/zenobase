package com.zenobase.commands;

import javax.inject.Inject;

import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
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
		return new Task(getParameter(TASK));
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
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 2:
					DeleteTaskCommand c = new DeleteTaskCommand(node);
					if ("withings".equalsIgnoreCase(c.getTask().getType())) {
						Logger.warn("from: {}", c.toJson());
						Task.TYPE.setValue(c.getTask().toJson(), "withings-weight");
						Logger.warn("to: {}", c.toJson());
					} else if ("fitbit".equalsIgnoreCase(c.getTask().getType())) {
						Logger.warn("from: {}", c.toJson());
						Task.TYPE.setValue(c.getTask().toJson(), "fitbit-steps");
						Logger.warn("to: {}", c.toJson());
					}
					return c;
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
