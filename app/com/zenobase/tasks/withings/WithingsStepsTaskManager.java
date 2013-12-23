package com.zenobase.tasks.withings;

import javax.inject.Inject;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.elasticsearch.common.primitives.Ints;
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

public class WithingsStepsTaskManager extends OAuthTaskManager {

	@Inject
	public WithingsStepsTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsTask.TYPE, credentialsManager);
	}

	@Override
	public WithingsStepsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		Unit<Mass> unit = Measures.<Mass>parseUnit(Objects.firstNonNull(settings.path("unit").textValue(), "km"));
		WithingsStepsTask task = new WithingsStepsTask(bucketId, principal, marker);
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
		return execute(task.as(WithingsStepsTask.class), credentials);
	}

	private Command execute(WithingsStepsTask task, OAuthCredentials credentials) {
		OAuthRequest request = createRequest(task, credentials);
		Response response = send(request, credentials);
		WithingsStepsResult result = new WithingsStepsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>: %s", result.getStatus(), task.getId());
		return createCommand(task, result);
	}

	private OAuthRequest createRequest(WithingsStepsTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/v2/measure");
		request.addQuerystringParameter("action", "getintradayactivity");
		request.addQuerystringParameter("userid", credentials.getScope());
		// if (task.getMarker() != null) { }
		request.addQuerystringParameter("startdate", format(DateTime.parse("2013-12-21T00:00:00+01:00")));
		request.addQuerystringParameter("enddate",   format(DateTime.parse("2013-12-21T17:07:31+01:00")));
		return request;
	}

	private static String format(DateTime time) {
		return Integer.toString(Ints.checkedCast(time.getMillis() / 1000));
	}

	private static Command createCommand(WithingsStepsTask task, WithingsStepsResult result) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings-steps task", "reverted withings-steps task");
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
