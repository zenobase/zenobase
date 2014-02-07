package com.zenobase.tasks.moves;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public class MovesPlacesTaskManager extends OAuthTaskManager {

	@Inject
	public MovesPlacesTaskManager(MovesCredentialsManager credentialsManager) {
		super(MovesPlacesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = LocalDate.parse(settings.path("marker").textValue()).toString();
		return new MovesPlacesTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MovesPlacesTask.class), credentials);
	}

	private Command execute(MovesPlacesTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime from = DateTime.parse(task.getMarker());
		List<Event> events = getEvents(task, credentials, from);
		return createCommand(task, credentials, events, token);
	}

	private List<Event> getEvents(MovesPlacesTask task, OAuthCredentials credentials, DateTime begin) {
		List<Event> events = Lists.newArrayList();
		LocalDate today = LocalDate.now(begin.getZone());
		for (LocalDate from = begin.toLocalDate(); !from.isAfter(today); from = from.withDayOfMonth(1).plusMonths(1)) {
			LocalDate to = from.dayOfMonth().withMaximumValue();
			if (to.isAfter(today)) {
				to = today;
			}
			PlacesQuery request = new PlacesQuery(begin, task.getPrincipal(), credentials);
			events.addAll(request.find(from, to).getEvents());
		}
		return events;
	}

	private class PlacesQuery {

		private final DateTime begin;
		private final Identity principal;
		private final OAuthCredentials credentials;

		public PlacesQuery(DateTime begin, Identity principal, OAuthCredentials credentials) {
			this.begin = begin;
			this.principal = principal;
			this.credentials = credentials;
		}

		public PlacesResult find(LocalDate from, LocalDate to) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.moves-app.com/api/1.1/user/places/daily");
			request.addQuerystringParameter("from", from.toString());
			request.addQuerystringParameter("to", to.toString());
			Response response = send(request, credentials);
			return new PlacesResult(principal, begin, parseArray(response));
		}
	}

	private Command createCommand(MovesPlacesTask task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran moves-places task", "reverted moves-places task");
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
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}

	private static String getMarker(List<Event> events) {
		return Iterables.getLast(events).getValue(Event.TIMESTAMP).plusSeconds(1).toString();
	}
}
