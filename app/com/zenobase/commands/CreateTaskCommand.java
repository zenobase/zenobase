package com.zenobase.commands;

import java.util.Map;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Maps;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class CreateTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create task", 2);
	private static final ObjectField TASK = new ObjectField("task");

	private CreateTaskCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
	}

	public CreateTaskCommand(Identity principal, Task task) {
		super(TYPE, principal);
		setParameter(TASK, task.toJson());
	}

	public Task getTask() {
		return new Task(getParameter(TASK));
	}

	@Override
	public Command reverse(Identity principal) {
		return new DeleteTaskCommand(principal, getTask());
	}

	@Override
	public String toString() {
		return String.format("created a task");
	}

	public static Map<String, String> taskToCredentials = Maps.newHashMap();

	public static class Parser extends CommandParser {

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 2: return new CreateTaskCommand(node);
			}
			return null;
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
			repository.store(command.getTask(), command.getTimestamp());
		}
	}
}
