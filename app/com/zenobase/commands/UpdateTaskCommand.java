package com.zenobase.commands;

import org.codehaus.jackson.node.ObjectNode;
import com.google.inject.Inject;

import com.zenobase.json.DomainNode;
import com.zenobase.json.ObjectField;
import com.zenobase.models.Identity;
import com.zenobase.services.TaskRepository;
import com.zenobase.tasks.Task;

public class UpdateTaskCommand extends Command {

	private static final Command.Type TYPE = new Command.Type("update task", 1);
	private static final ObjectField FROM = new ObjectField("from");
	private static final ObjectField TO = new ObjectField("to");

	private UpdateTaskCommand(ObjectNode node) {
		super(node);
	}

	public UpdateTaskCommand(Identity principal, DomainNode from, DomainNode to) {
		super(TYPE, principal);
		setParameter(FROM, from.toJson());
		setParameter(TO, to.toJson());
	}

	public Task getFrom() {
		return new Task(getParameter(FROM));
	}

	public Task getTo() {
		return new Task(getParameter(TO));
	}

	@Override
	public Command reverse(Identity principal) {
		Task from = getTo();
		Task to = getFrom();
		from.setVersion(from.getVersion() + 1);
		to.setVersion(to.getVersion() + 1);
		return new UpdateTaskCommand(principal, from, to);
	}

	@Override
	public String toString() {
		return String.format("updated task %s", getFrom().getId());
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
			repository.update(command.getTo().copy()); // copy to prevent the version number from being incremented
		}
	}
}
