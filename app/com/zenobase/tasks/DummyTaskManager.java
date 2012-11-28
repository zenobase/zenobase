package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class DummyTaskManager extends TaskManager {

	@Override
	public String getType() {
		return DummyTask.TYPE;
	}

	@Override
	public Task newTask(String bucketId, Identity principal) {
		Task task = new DummyTask(bucketId, principal, null);
		task.setState(Task.State.UNAUTHORIZED);
		return task;
	}

	@Override
	public String getAuthorizationUrl(Task task) {
		return String.format("http://localhost:9000/#/buckets/%s/tasks/%s/auth/?tag=%s", task.getBucketId(), task.getId(), "test");
	}

	@Override
	public Command authorize(Task task, ObjectNode config) {
		String tag = DummyTask.TAG.getValue(config);
		if (tag == null) {
			return null;
		}
		DummyTask to = new DummyTask(task.copy().toJson());
		to.setTag(tag);
		to.setState(Task.State.READY);
		return new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to);
	}

	@Override
	public Command execute(Task task) {
		return execute(new DummyTask(task.toJson()));
	}

	private Command execute(DummyTask task) {
		Preconditions.checkState(task.getState() == Task.State.READY, "Task is not ready: %s", task.getId());
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "created a dummy event", "removed a dummy event");
		Event event = new Event();
		event.setValue(Event.AUTHOR, task.getPrincipal());
		event.setValue(Event.TIMESTAMP, new DateTime(DateTimeZone.UTC));
		event.setValue(Event.TAG, task.getTag());
		DummyTask to = task.copy();
		to.setUpdated(new DateTime(DateTimeZone.UTC));
		command.add(new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to));
		command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		return command;
	}
}
