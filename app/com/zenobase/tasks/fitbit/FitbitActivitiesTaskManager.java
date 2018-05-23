package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.Duration;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class FitbitActivitiesTaskManager extends FitbitTaskManagerSupport<FitbitActivitiesTask> {

	@Inject
	public FitbitActivitiesTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitActivitiesTask.TYPE, FitbitActivitiesTask.class, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		boolean autodetected = settings.path("autodetected").booleanValue();
		return new FitbitActivitiesTask(bucketId, principal, marker, autodetected);
	}

	@Override
	protected Command safeExecute(FitbitActivitiesTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = Lists.newArrayList();
		FitbitProfileResult profile = getProfile(credentials);
		DateTime afterDate = DateTime.parse(task.getMarker());
		for (String url = "https://api.fitbit.com/1/user/-/activities/list.json"; url != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			request.addHeader("Accept-Language", profile.getDistanceLocale());
			if (events.isEmpty()) {
				request.addQuerystringParameter("offset", "0");
				request.addQuerystringParameter("limit", "100");
				request.addQuerystringParameter("sort", "asc");
				request.addQuerystringParameter("afterDate", afterDate.toString("yyyy-MM-dd'T'HH:mm:ss"));
			}
			try {
				Response response = send(request, credentials);
				JsonNode node = parseObject(response);
				FitbitActivitiesResult result = new FitbitActivitiesResult(node, task.getPrincipal(), task.includeAutodetected(), profile.getDistanceUnit());
				events.addAll(result.getEvents());
				url = result.next();
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					Logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
					break;
				}
				throw e;
			}

		}
		return createCommand(task, credentials, events, Objects.firstNonNull(getMarker(events), task.getMarker()), token);
	}

	private static String getMarker(Iterable<Event> events) {
		DateTime latest = null;
		for (Event event : events) {
			Duration duration = event.getValue(Event.DURATION);
			DateTime time = event.getValue(Event.TIMESTAMP).plus(duration);
			if (latest == null || time.isAfter(latest)) {
				latest = time;
			}
		}
		return latest != null ? latest.toString() : null;
	}
}
