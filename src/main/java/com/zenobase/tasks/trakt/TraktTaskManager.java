package com.zenobase.tasks.trakt;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class TraktTaskManager extends OAuthTaskManager {

	private static final String host = "https://api.trakt.tv";

	@Inject
	public TraktTaskManager(TraktCredentialsManager credentialsManager) {
		super(TraktTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new TraktTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(TraktTask.class), credentials);
	}

	private Command execute(TraktTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}

		TraktSettingsResult settings = getSettings(credentials);
		List<Event> events = new ArrayList<>();
		DateTime after = parseMarker(task.getMarker());
		addEvents("movies", credentials, task, settings, after, events);
		addEvents("episodes", credentials, task, settings, after, events);
		return createCommand(task, credentials, events, Objects.requireNonNull(token));
	}

	private TraktSettingsResult getSettings(OAuthCredentials credentials) {
		var request = new OAuthRequest(Verb.GET, host + "/users/settings");
		Response response = send(request, credentials);
		return new TraktSettingsResult(parseObject(response));
	}

	private void addEvents(
		String type,
		OAuthCredentials credentials,
		TraktTask task,
		TraktSettingsResult settings,
		@Nullable DateTime after,
		List<Event> events
	) {
		int limit = 10;
		for (int page = 1; page < 100; ++page) {
			var request = new OAuthRequest(Verb.GET, host + "/users/me/history/" + type);
			request.addQuerystringParameter("limit", Integer.toString(limit));
			request.addQuerystringParameter("page", Integer.toString(page));
			request.addQuerystringParameter("extended", "full");
			Response response = send(request, credentials);
			TraktHistoryResult result = new TraktHistoryResult(
				parseArray(response),
				task.getPrincipal(),
				after,
				settings.getTimeZone()
			);
			List<Event> add = result.getEvents();
			events.addAll(add);
			if (add.size() < limit) {
				break;
			}
		}
	}

	static @Nullable DateTime parseMarker(@Nullable String marker) {
		return marker != null ? DateTime.parse(marker) : null;
	}

	static @Nullable String formatMarker(@Nullable DateTime time) {
		return time != null ? time.toString() : null;
	}

	static @Nullable String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			DateTime time = Objects.requireNonNull(event.getValue(Event.TIMESTAMP));
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}

	private Command createCommand(
		TraktTask task,
		OAuthCredentials credentials,
		List<Event> events,
		Token expiredToken
	) {
		var command = new CompoundCommand(
			task.getPrincipal(),
			"ran " + getType() + " task",
			"reverted " + getType() + " task"
		);
		command.add(
			UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build()
		);
		if (!Objects.equals(credentials.getToken(), expiredToken)) {
			command.add(
				UpdateCredentialsCommand.builder(credentials)
					.with(Credentials.CREDENTIALS)
					.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
					.build()
			);
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
