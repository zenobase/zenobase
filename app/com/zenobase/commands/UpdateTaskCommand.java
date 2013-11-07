package com.zenobase.commands;

import javax.inject.Inject;

import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.json.JsonPatch;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.Task;

public class UpdateTaskCommand extends UpdateCommandSupport {

	private static final Command.Type TYPE = new Command.Type("update task", 3);

	private UpdateTaskCommand(ObjectNode node) {
		super(node);
		setType(TYPE);
		// checkType(TYPE);
	}

	private UpdateTaskCommand(Identity principal, String taskId, ObjectNode from, ObjectNode to) {
		super(TYPE, principal, taskId, from, to);
	}

	@Override
	protected Command newInstance(Identity principal, String objectId, ObjectNode from, ObjectNode to) {
		return new UpdateTaskCommand(principal, objectId, from, to);
	}

	public Task apply(Task task) {
		return new Task(new JsonPatch(getFrom(), getTo()).apply(task.toJson()));
	}

	@Override
	public String toString() {
		return String.format("updated task %s", getObjectId());
	}

	public static Builder builder(final Task task) {
		return new Builder() {
			@Override
			public UpdateTaskCommand build() {
				return new UpdateTaskCommand(task.getPrincipal(), task.getId(), getFrom(), getTo());
			}
		};
	}

	public static class Parser extends CommandParser {

		private final TaskRepository tasks;
		private final CredentialsRepository credentials;

		@Inject
		public Parser(TaskRepository tasks, CredentialsRepository credentials) {
			this.tasks = tasks;
			this.credentials = credentials;
		}

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 2: return migrate(node);
				case 3: return new UpdateTaskCommand(node);
			}
			return null;
		}

		private Command migrate(ObjectNode node) {
			Logger.info("\nmigrating 'update task'...");
			Logger.info("< " + node);
			ObjectNode original = node.deepCopy();
			Identity principal = Command.PRINCIPAL.getValue(node);
			ObjectNode fromCredentials = split(UpdateTaskCommand.FROM.getValue(PARAMETERS.getValue(node)));
			ObjectNode toCredentials = split(UpdateTaskCommand.TO.getValue(PARAMETERS.getValue(node)));
			if (fromCredentials == null && toCredentials == null) {
				Logger.info("> " + new UpdateTaskCommand(node).toJson());
				return new UpdateTaskCommand(node);
			} else {
				String id = findCredentialsId(OBJECT_ID.getValue(node));
				Command command = new UpdateCredentialsCommand(principal, id, fromCredentials, toCredentials);
				Migration.copy(Command.TIMESTAMP, node, command.toJson());
				Logger.info("> " + command.toJson());
				if (original.equals(node)) {
					return command;
				}
				CompoundCommand commands = new CompoundCommand(principal, "update task and credentials", "update task and credentials");
				Logger.info("> " + new UpdateTaskCommand(node).toJson());
				commands.add(new UpdateTaskCommand(node));
				commands.add(command);
				return commands;
			}
		}

		public static ObjectNode split(ObjectNode taskNode) {
			String url = Credentials.AUTHORIZATION_URL.getValue(taskNode);
			ObjectNode config = Credentials.CREDENTIALS.getValue(taskNode);
			if (config == null && url == null) {
				return null;
			}
			ObjectNode credentialsNode = Nodes.newObject();
			if (config != null) {
				if (config.get("userId") != null) {
					config.put("scope", config.get("userId").asText());
				}
			}
			Credentials.CREDENTIALS.setValue(credentialsNode, config);
			Migration.copy(Credentials.AUTHORIZATION_URL, taskNode, credentialsNode);
			Credentials.AUTHORIZATION_URL.setValue(taskNode, null);
			Credentials.CREDENTIALS.setValue(taskNode, null);
			return credentialsNode;
		}

		private String findCredentialsId(String taskId) {
			Task task = Preconditions.checkNotNull(tasks.find(taskId));
			return Preconditions.checkNotNull(credentials.find(task.getPrincipal(), task.getType().replaceAll("-.*", ""))).getId();
		}
	}

	public static class Handler extends CommandHandler<UpdateTaskCommand> {

		private final TaskRepository repository;

		@Inject
		public Handler(TaskRepository repository) {
			super(UpdateTaskCommand.class);
			this.repository = repository;
		}

		@Override
		public void executeTyped(UpdateTaskCommand command) {
			Task task = repository.find(command.getObjectId());
			Preconditions.checkNotNull(task, "Can't find task: %s", command.getObjectId());
			repository.update(command.apply(task), command.getTimestamp());
		}
	}
}
