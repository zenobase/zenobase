package com.zenobase.tasks.trakt;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

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

public class TraktTaskManager extends OAuthTaskManager {

	private static final String host = "https://api-v2launch.trakt.tv";

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
		List<Event> events = Lists.newArrayList();
		DateTime after = parseMarker(task.getMarker());
		addEvents("movies", credentials, task, settings, after, events);
		addEvents("episodes", credentials, task, settings, after, events);
		return createCommand(task, credentials, events, token);
	}

	private TraktSettingsResult getSettings(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, host + "/users/settings");
		Response response = send(request, credentials);
		return new TraktSettingsResult(parseObject(response));
	}

	private void addEvents(String type, OAuthCredentials credentials, TraktTask task, TraktSettingsResult settings, DateTime after, List<Event> events) {
		final int limit = 10;
		for (int page = 1; page < 100; ++page) {
			OAuthRequest request = new OAuthRequest(Verb.GET, host + "/users/me/history/" + type);
			request.addQuerystringParameter("limit", Integer.toString(limit));
			request.addQuerystringParameter("page", Integer.toString(page));
			request.addQuerystringParameter("extended", "full");
			Response response = send(request, credentials);
			TraktHistoryResult result = new TraktHistoryResult(parseArray(response), task.getPrincipal(), after, settings.getTimeZone());
			List<Event> add = result.getEvents();
			events.addAll(add);
			if (add.size() < limit) {
				break;
			}
		}
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

	private Command createCommand(TraktTask task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events))
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!Objects.equal(credentials.getToken(), expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
				.build());
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
