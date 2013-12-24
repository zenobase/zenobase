package com.zenobase.tasks.withings;

import javax.inject.Inject;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import play.Logger;
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
		super(WithingsStepsTask.TYPE, credentialsManager);
	}

	@Override
	public WithingsStepsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue());
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		Unit<Mass> unit = Measures.<Mass>parseUnit(Objects.firstNonNull(settings.path("unit").textValue(), "km"));
		WithingsStepsTask task = new WithingsStepsTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setUnit(unit);
		return task;
	}

	private static String parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate().toString() : null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(WithingsStepsTask.class), credentials);
	}

	private Command execute(WithingsStepsTask task, OAuthCredentials credentials) {
		Logger.info("-Doauth.token=" + credentials.getToken().getToken());
		Logger.info("-Doauth.secret=" + credentials.getToken().getSecret());
		OAuthRequest request = createRequest(task, credentials);
		Response response = send(request, credentials);
		WithingsStepsResult result = new WithingsStepsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getDistanceUnit(), task.getHeightUnit());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>: %s", result.getStatus(), task.getId(), response.getBody());
		return createCommand(task, result.getEvents());
	}

	private OAuthRequest createRequest(WithingsStepsTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/v2/measure");
		request.addQuerystringParameter("action", "getactivity");
		request.addQuerystringParameter("userid", credentials.getScope());
		request.addQuerystringParameter("startdateymd", LocalDate.parse(task.getMarker()).toString());
		request.addQuerystringParameter("enddateymd", LocalDate.now().toString());
		return request;
	}

	private static Command createCommand(WithingsStepsTask task, Iterable<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings-steps task", "reverted withings-steps task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), next(task.getMarker(), events))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static String next(String marker, Iterable<Event> events) {
		return next(LocalDate.parse(marker), events).toString();
	}

	private static LocalDate next(LocalDate marker, Iterable<Event> events) {
		for (Event event : events) {
			LocalDate date = event.getValue(Event.TIMESTAMP).toLocalDate().plusDays(1);
			if (date.isAfter(marker)) {
				marker = date;
			}
		}
		return marker;
	}
}
