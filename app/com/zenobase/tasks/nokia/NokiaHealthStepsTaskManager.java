package com.zenobase.tasks.nokia;

import java.util.List;

import javax.inject.Inject;
import javax.measure.quantity.Length;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
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
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class NokiaHealthStepsTaskManager extends OAuthTaskManager {

	@Inject
	public NokiaHealthStepsTaskManager(NokiaHealthCredentialsManager credentialsManager) {
		super(NokiaHealthStepsTask.TYPE, credentialsManager);
	}

	@Override
	public NokiaHealthStepsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		Unit<Length> lengthUnit = Objects.firstNonNull(new UnitField<Length>("unit").getValue(settings), Units.KM);
		String marker = parseMarker(settings.path("marker").textValue());
		return new NokiaHealthStepsTask(bucketId, principal, tag, lengthUnit, Units.KCAL, marker);
	}

	private static String parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate().toString() : null;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(NokiaHealthStepsTask.class), credentials);
	}

	private Command execute(NokiaHealthStepsTask task, OAuthCredentials credentials) {
		OAuthRequest request = createRequest(task, credentials);
		Response response = send(request, credentials);
		NokiaHealthStepsResult result = new NokiaHealthStepsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getDistanceUnit(), task.getHeightUnit(), task.getEnergyUnit());
		Preconditions.checkState(result.getStatus() == 0, "Expected status <0> but got <%s> for task <%s>", result.getStatus(), task.getId());
		return createCommand(task, result.getEvents());
	}

	private OAuthRequest createRequest(NokiaHealthStepsTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.health.nokia.com/v2/measure");
		request.addQuerystringParameter("action", "getactivity");
		request.addQuerystringParameter("userid", credentials.getScope());
		request.addQuerystringParameter("startdateymd", LocalDate.parse(task.getMarker()).toString());
		request.addQuerystringParameter("enddateymd", LocalDate.now().toString());
		return request;
	}

	private static Command createCommand(NokiaHealthStepsTask task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran nokia-steps task", "reverted nokia-steps task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), next(task.getMarker(), events))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
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
