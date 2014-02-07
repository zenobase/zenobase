package com.zenobase.tasks.moves;

import java.util.Iterator;
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
import com.google.common.util.concurrent.RateLimiter;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.foursquare.FoursquareVenue;
import com.zenobase.tasks.foursquare.FoursquareVenues;

public class MovesPlacesTaskManager extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(1);

	private final FoursquareVenues venues;

	@Inject
	public MovesPlacesTaskManager(MovesCredentialsManager credentialsManager, FoursquareVenues venues) {
		super(MovesPlacesTask.TYPE, credentialsManager);
		this.venues = venues;
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
		removeDuplicates(events);
		removeLast(events);
		resolveFoursquareVenues(events);
		return createCommand(task, credentials, events, token);
	}

	private List<Event> getEvents(MovesPlacesTask task, OAuthCredentials credentials, DateTime begin) {
		List<Event> events = Lists.newArrayList();
		LocalDate today = LocalDate.now(begin.getZone());
		for (LocalDate from = begin.toLocalDate(); !from.isAfter(today); from = from.withDayOfMonth(1).plusMonths(1)) {
			checkRateLimit();
			LocalDate to = min(from.dayOfMonth().withMaximumValue(), today);
			PlacesQuery request = new PlacesQuery(begin, task.getPrincipal(), credentials);
			events.addAll(request.find(from, to).getEvents());
		}
		return events;
	}

	private static LocalDate min(LocalDate a, LocalDate b) {
		return a.isAfter(b) ? b : a;
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

	private void removeDuplicates(List<Event> events) {
		DateTime t0 = null;
		for (Iterator<Event> i = events.iterator(); i.hasNext();) {
			Event event = i.next();
			DateTime t1 = event.getValue(Event.TIMESTAMP);
			if (t0 == null || !t0.equals(t1)) {
				t0 = t1;
			} else {
				i.remove();
			}
		}
	}

	private void removeLast(List<Event> events) {
		if (!events.isEmpty()) {
			events.remove(events.size() - 1);
		}
	}

	private void resolveFoursquareVenues(Iterable<Event> events) {
		for (Event event : events) {
			Resource resource = event.getValue(Event.RESOURCE);
			if (resource != null) {
				FoursquareVenue venue = venues.find(resource.getTitle());
				if (venue != null) {
					event.setValue(Event.RESOURCE, venue.toResource());
					for (String tag : venue.getCategories()) {
						event.addValue(Event.TAG, tag);
					}
				}
			}
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
		Event latest = Iterables.getLast(events);
		return latest.getValue(Event.TIMESTAMP).plus(latest.getValue(Event.DURATION)).toString();
	}

	private static void checkRateLimit() {
		RATE_LIMITER.acquire();
	}
}
