package com.zenobase.tasks.moves;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MovesActivitiesTaskManager extends MovesTaskManagerSupport {

	@Inject
	public MovesActivitiesTaskManager(MovesCredentialsManager credentialsManager) {
		super(MovesActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = LocalDate.parse(settings.path("marker").textValue()).toString();
		return new MovesActivitiesTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MovesActivitiesTask.class), credentials);
	}

	private Command execute(MovesActivitiesTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime from = DateTime.parse(task.getMarker());
		List<Event> events = getEvents(task, credentials, from);
		removeDuplicates(events);
		removeLast(events);
		return createCommand(task, credentials, events, token);
	}

	private List<Event> getEvents(MovesActivitiesTask task, OAuthCredentials credentials, DateTime begin) {
		List<Event> events = Lists.newArrayList();
		LocalDate today = LocalDate.now(begin.getZone());
		for (LocalDate from = begin.toLocalDate(); !from.isAfter(today); from = from.withDayOfMonth(1).plusMonths(1)) {
			checkRateLimit();
			LocalDate to = min(from.dayOfMonth().withMaximumValue(), today);
			ActivitiesQuery request = new ActivitiesQuery(begin, task.getPrincipal(), credentials);
			events.addAll(request.find(from, to).getEvents());
		}
		return events;
	}

	private class ActivitiesQuery {

		private final DateTime begin;
		private final Identity principal;
		private final OAuthCredentials credentials;

		public ActivitiesQuery(DateTime begin, Identity principal, OAuthCredentials credentials) {
			this.begin = begin;
			this.principal = principal;
			this.credentials = credentials;
		}

		public ActivitiesResult find(LocalDate from, LocalDate to) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.moves-app.com/api/1.1/user/activities/daily");
			request.addQuerystringParameter("from", from.toString());
			request.addQuerystringParameter("to", to.toString());
			Response response = send(request, credentials);
			return new ActivitiesResult(principal, begin, parseArray(response));
		}
	}
}
