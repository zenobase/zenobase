package com.zenobase.commands;

import java.util.Map;

import org.elasticsearch.common.collect.Maps;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;

public class CreateTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("create task", 2);
	private static final ObjectField TASK = new ObjectField("task");

	private CreateTaskCommand(ObjectNode node) {
		super(node);
		setType(TYPE);
		// checkType(TYPE);
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
				case 1: return migrate(node);
				case 2: return new CreateTaskCommand(node);
			}
			return null;
		}

		private Command migrate(ObjectNode node) {
			Logger.info("\nmigrating 'create task'...");
			Logger.info("< " + node);
			Identity principal = Command.PRINCIPAL.getValue(node);
			ObjectNode credentials = Migration.splitCredentials(TASK.getValue(PARAMETERS.getValue(node)));
			if (credentials == null) {
				Logger.info("> " + new CreateTaskCommand(node).toJson());
				return new CreateTaskCommand(node);
			} else {
				CompoundCommand commands = new CompoundCommand(principal, "create task and credentials", "delete task and credentials");
				Migration.copy(Command.TIMESTAMP, node, commands.toJson());
				Logger.info("> " + new CreateTaskCommand(node).toJson());
				commands.add(new CreateTaskCommand(node));
				Credentials cred = new Credentials(credentials);
				String taskId = Preconditions.checkNotNull(Task.ID.getValue(TASK.getValue(PARAMETERS.getValue(node))));
				taskToCredentials.put(taskId, cred.getId());
				Command command = new CreateCredentialsCommand(principal, cred);
				Migration.copy(Command.TIMESTAMP, node, command.toJson());
				Logger.info("> " + command.toJson());
				commands.add(command);
				return commands;
			}
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
