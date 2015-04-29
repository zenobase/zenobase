package com.zenobase.tasks.moodpanda;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.w3c.dom.Document;
import play.libs.ws.WS;
import play.libs.ws.WSRequestHolder;
import play.libs.ws.WSResponse;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.TaskManager;

public class MoodPandaTaskManager extends TaskManager {

	private static final String HOST = "http://www.moodpanda.com/api";

	private final String apiKey;

	@Inject
	public MoodPandaTaskManager(@Named("moodpanda.api.key") String apiKey) {
		super(MoodPandaTask.TYPE);
		this.apiKey = apiKey;
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTime marker = parseMarker(settings.path("marker").textValue());
		DateTime begin = DateTime.now(marker.getZone()).minusYears(1).plusDays(1);
		if (marker.isBefore(begin)) {
			marker = begin;
		}
		String email = Preconditions.checkNotNull(settings.path("email").textValue());
		String tag = Preconditions.checkNotNull(settings.path("tag").textValue());
		return new MoodPandaTask(bucketId, principal, email, tag, formatMarker(marker));
	}

	@Override
	public Command execute(Task task) {
		return execute(task.as(MoodPandaTask.class));
	}

	private Command execute(MoodPandaTask task) {
		DateTime from = parseMarker(task.getMarker());
		MoodPandaUserResult user = new MoodPandaUserResult(fetchUser(task.getEmail()));
		MoodPandaFeedResult result = new MoodPandaFeedResult(fetchFeed(user.getUserId(), from),
			task.getPrincipal(), user.getOffset(), task.getTag());
		return createCommand(task, result.getEvents(from));
	}

	private Document fetchUser(String email) {
		return fetch(WS.url(HOST + "/user/data.ashx")
			.setQueryParameter("email", email)
			.setQueryParameter("format", "xml")
			.setQueryParameter("key", apiKey));
	}

	private Document fetchFeed(String userId, DateTime from) {
		return fetch(WS.url(HOST + "/user/feed/data.ashx")
			.setQueryParameter("userid", userId)
			.setQueryParameter("from", from.toLocalDate().toString())
			.setQueryParameter("to", LocalDate.now(from.getZone()).toString())
			.setQueryParameter("format", "xml")
			.setQueryParameter("dateorder", "asc")
			.setQueryParameter("key", apiKey));
	}

	private Document fetch(WSRequestHolder request) {
		WSResponse response = request.get().get(10000L);
		Preconditions.checkState(response.getHeader("Content-Type").startsWith("text/xml"),
			"Couldn't request <%s>: %s", response.getUri(), response.getBody());
		return response.asXml();
	}

	static DateTime parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static String formatMarker(DateTime time) {
		return time != null ? time.toString() : null;
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

	private Command createCommand(MoodPandaTask task, List<Event> events) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
