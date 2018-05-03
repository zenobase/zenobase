package com.zenobase.commands;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.zenobase.json.JsonPatch;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

import javax.inject.Inject;

public class UpdateTaskCommand extends UpdateCommandSupport {

	private static final Command.Type TYPE = new Command.Type("update task", 3);

	private UpdateTaskCommand(ObjectNode node) {
		super(node);
		checkType(TYPE);
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

		@Override
		public String getTypeName() {
			return TYPE.getName();
		}

		@Override
		public Command parse(ObjectNode node, int version) {
			switch (version) {
				case 3: return new UpdateTaskCommand(node);
			}
			return null;
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
