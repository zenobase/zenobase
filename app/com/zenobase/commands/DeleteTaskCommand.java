package com.zenobase.commands;

import javax.inject.Inject;

import play.Logger;
import com.fasterxml.jackson.databind.JsonNode;
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

			JsonNode settings = node.path("parameters").path("task").path("settings");
			if (settings.isObject()) {
				if (((ObjectNode) settings).remove("key") != null) {
					Logger.warn("Deleted obsolete API key");
				}
			}

			switch (version) {
				case 2: return new DeleteTaskCommand(node);
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
