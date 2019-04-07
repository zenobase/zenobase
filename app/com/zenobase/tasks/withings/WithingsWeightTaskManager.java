package com.zenobase.tasks.withings;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidCredentialsException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class WithingsWeightTaskManager extends WithingsTaskManagerSupport<WithingsWeightTask> {

	@Inject
	public WithingsWeightTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsWeightTask.TYPE, WithingsWeightTask.class, credentialsManager);
	}

	@Override
	public WithingsWeightTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "body");
		Unit<Mass> unit = Objects.firstNonNull(new UnitField<Mass>("unit").getValue(settings), Units.KG);
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		return new WithingsWeightTask(bucketId, principal, tag, unit, timezone, marker);
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? Long.toString(LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).getMillis() / 1000) : null;
	}

	@Override
	Command safeExecute(WithingsWeightTask task, OAuthCredentials credentials) {
		OAuthRequest request = createRequest(task);
		Response response = send(request, credentials);
		WithingsWeightResult result = new WithingsWeightResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
		if (result.getStatus() == 401) {
			throw new InvalidCredentialsException(credentials);
		}
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return createCommand(task, result);
	}

	private OAuthRequest createRequest(WithingsWeightTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/measure");
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("category", "1"); // actual measurements
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker());
		}
		return request;
	}

	private static Command createCommand(WithingsWeightTask task, WithingsWeightResult result) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran withings-weight task", "reverted withings-weight task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
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
