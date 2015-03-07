package com.zenobase.tasks.withings;

import java.util.List;

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
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class WithingsWeightTaskManager extends OAuthTaskManager {

	@Inject
	public WithingsWeightTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsWeightTask.TYPE, credentialsManager);
	}

	@Override
	public WithingsWeightTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "body");
		Unit<Mass> unit = Objects.firstNonNull(new UnitField<Mass>("unit").getValue(settings), Units.KG);
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		WithingsWeightTask task = new WithingsWeightTask(bucketId, principal, tag, unit, timezone, marker);
		return task;
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? Long.toString(LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).getMillis() / 1000) : null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(WithingsWeightTask.class), credentials);
	}

	private Command execute(WithingsWeightTask task, OAuthCredentials credentials) {
		OAuthRequest request = createRequest(task, credentials);
		Response response = send(request, credentials);
		WithingsWeightResult result = new WithingsWeightResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return createCommand(task, result);
	}

	private OAuthRequest createRequest(WithingsWeightTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "http://wbsapi.withings.net/measure");
		request.addQuerystringParameter("userid", credentials.getScope());
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("category", "1"); // actual measurements
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker().toString());
		}
		return request;
	}

	private static Command createCommand(WithingsWeightTask task, WithingsWeightResult result) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings task", "reverted withings task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), result.getMarker())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		List<Event> events = result.getEvents();
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
