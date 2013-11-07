package com.zenobase.commands;

import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;

public class DeleteTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("delete task", 2);
	private static final ObjectField TASK = new ObjectField("task");

	private DeleteTaskCommand(ObjectNode node) {
		super(node);
		setType(TYPE);
		// checkType(TYPE);
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
				case 1: return migrate(node);
				case 2: return new DeleteTaskCommand(node);
			}
			return null;
		}

		private Command migrate(ObjectNode node) {
			Logger.info("\nmigrating 'delete task'...");
			Logger.info("< " + node);
			Identity principal = Command.PRINCIPAL.getValue(node);
			ObjectNode credentials = Migration.splitCredentials(TASK.getValue(PARAMETERS.getValue(node)));
			if (credentials == null) {
				Logger.info("> " + new DeleteTaskCommand(node).toJson());
				return new DeleteTaskCommand(node);
			} else {
				CompoundCommand commands = new CompoundCommand(principal, "delete task and credentials", "create task and credentials");
				Migration.copy(Command.TIMESTAMP, node, commands.toJson());
				Logger.info("> " + new DeleteTaskCommand(node).toJson());
				commands.add(new DeleteTaskCommand(node));
				Command command = new CreateCredentialsCommand(principal, new Credentials(credentials));
				Migration.copy(Command.TIMESTAMP, node, command.toJson());
				Logger.info("> " + command.toJson());
				commands.add(command);
				return commands;
			}
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
