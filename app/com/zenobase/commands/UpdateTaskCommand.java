package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.inject.Inject;

import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class UpdateTaskCommand extends UpdateCommandSupport {

	private static final Command.Type TYPE = new Command.Type("update task", 1);

	private UpdateTaskCommand(ObjectNode node) {
		super(node);
	}

	public UpdateTaskCommand(Identity principal, String taskId, Iterable<Change> patches) {
		super(TYPE, principal, taskId, patches);
	}

	public Task apply(Task task) {
		Task changed = task.copy();
		for (Change change : getChanges()) {
			change.apply(changed.toJson().with(Task.CONFIG.getName()));
		}
		return changed;
	}

	@Override
	public String toString() {
		return String.format("updated task %s", getObjectId());
	}

	public static Builder<UpdateTaskCommand> builder(final Task task) {
		return new Builder<UpdateTaskCommand>() {
			@Override
			public UpdateTaskCommand build() {
				return new UpdateTaskCommand(task.getPrincipal(), task.getId(), getPatches());
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
