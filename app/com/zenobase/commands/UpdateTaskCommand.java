package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.zenobase.json.Field;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class UpdateTaskCommand extends UpdateCommandSupport {

	private static final Command.Type TYPE = new Command.Type("update task", 1);

	private UpdateTaskCommand(ObjectNode node) {
		super(node);
	}

	private UpdateTaskCommand(Identity principal, String taskId, String field, Iterable<UpdateCommandSupport.Change> patches) {
		super(TYPE, principal, taskId, field, patches);
	}

	@Override
	protected Command newInstance(Identity principal, String objectId, String field, Iterable<UpdateCommandSupport.Change> patches) {
		return new UpdateTaskCommand(principal, objectId, field, patches);
	}

	public Task apply(Task task) {
		Task changed = task.copy();
		for (UpdateCommandSupport.Change change : getChanges()) {
			change.apply(changed.toJson().with(Task.SETTINGS.getName()));
		}
		return changed;
	}

	@Override
	public String toString() {
		return String.format("updated task %s", getObjectId());
	}

	public static Builder<UpdateTaskCommand> builder(final Task task) {
		return builder(task, null);
	}

	public static Builder<UpdateTaskCommand> builder(final Task task, final Field<?> field) {
		return new Builder<UpdateTaskCommand>() {
			@Override
			public UpdateTaskCommand build() {
				return new UpdateTaskCommand(task.getPrincipal(), task.getId(), field != null ? field.getName() : null, getPatches());
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
				case 1:
					return new UpdateTaskCommand(node);
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
			Task task = repository.findTask(command.getObjectId());
			Preconditions.checkNotNull(task, "Can't find task: %s", command.getObjectId());
			repository.update(command.apply(task));
		}
	}
}
