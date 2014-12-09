package com.zenobase.tasks.trackthisforme;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class TrackthisformeTaskManager extends OAuthTaskManager {

	private static final String HOST = "https://www.trackthisfor.me/api/v1";

	@Inject
	public TrackthisformeTaskManager(TrackthisformeCredentialsManager credentialsManager) {
		super(TrackthisformeTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String category = settings.path("category").textValue();
		String field = settings.path("field").textValue();
		String unit = settings.path("unit").textValue();
		boolean rating = settings.path("rating").booleanValue();
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new TrackthisformeTask(bucketId, principal, category, field, unit, rating, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(TrackthisformeTask.class), credentials);
	}

	private Command execute(TrackthisformeTask task, OAuthCredentials credentials) {
		Category category = getCategory(task, credentials);
		if (category == null) {
			Logger.warn("Couldn't find category {} for task {}", task.getCategory(), task.getId());
			return null;
		}
		DateTime begin = DateTime.parse(task.getMarker());
		DateTime end = DateTime.now();
		if (end.isBefore(begin)) {
			return null;
		}
		OAuthRequest request = new OAuthRequest(Verb.GET, String.format("%s/categories/%s/elements/from/%s/to/%s",
			HOST, category.getId(), toString(begin), toString(end)));
		Response response = send(request, credentials);
		TrackthisformeElementsResult result = new TrackthisformeElementsResult(parseObject(response),
			task.getPrincipal(), begin, category, task.getField(), task.getUnit(), task.includeRatings());
		return createCommand(task, credentials, result.getEvents());
	}

	private Category getCategory(TrackthisformeTask task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/categories.json");
		Response response = send(request, credentials);
		return new TrackthisformeCategoriesResult(parseObject(response)).getCategory(task.getCategory());
	}

	static String toString(DateTime t) {
		return t.toString("MM/dd/yyyy");
	}

	static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = event.getValue(Event.TIMESTAMP);
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}

	private Command createCommand(Task task, OAuthCredentials credentials, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
