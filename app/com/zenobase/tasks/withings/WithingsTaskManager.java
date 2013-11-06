package com.zenobase.tasks.withings;

import javax.inject.Inject;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Measures;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class WithingsTaskManager extends OAuthTaskManager {

	@Inject
	public WithingsTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsTask.TYPE, credentialsManager);
	}

	@Override
	public WithingsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "body");
		Unit<Mass> unit = Measures.<Mass>parseUnit(Objects.firstNonNull(settings.path("unit").textValue(), "kg"));
		WithingsTask task = new WithingsTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setUnit(unit);
		task.setTimezone(timezone);
		return task;
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? Long.toString(LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).getMillis() / 1000) : null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(WithingsTask.class), credentials);
	}

	private Command execute(WithingsTask task, OAuthCredentials credentials) {
		OAuthRequest request = createRequest(task, credentials);
		Response response = send(request, credentials);
		WithingsResult result = new WithingsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return createCommand(task, result);
	}

	private OAuthRequest createRequest(WithingsTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/measure");
		request.addQuerystringParameter("userid", credentials.getScope());
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("devtype", "1"); // weight scale data
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker().toString());
		}
		return request;
	}

	private static Command createCommand(WithingsTask task, WithingsResult result) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings task", "reverted withings task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), result.getMarker())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : result.getEvents()) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
