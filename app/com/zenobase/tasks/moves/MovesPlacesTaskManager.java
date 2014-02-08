package com.zenobase.tasks.moves;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.models.Resource;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import com.zenobase.tasks.foursquare.FoursquareVenue;
import com.zenobase.tasks.foursquare.FoursquareVenues;

public class MovesPlacesTaskManager extends MovesTaskManagerSupport {

	private final FoursquareVenues venues;

	@Inject
	public MovesPlacesTaskManager(MovesCredentialsManager credentialsManager, FoursquareVenues venues) {
		super(MovesPlacesTask.TYPE, credentialsManager);
		this.venues = venues;
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		return new MovesPlacesTask(bucketId, principal);
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
		DateTime from = task.getFrom();
		if (from == null) {
			ProfileResult profile = getProfile(credentials);
			from = profile.getFirstDate();
		}
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
			LocalDate to = min(from.dayOfMonth().withMaximumValue(), today);
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
			OAuthRequest request = newRequest("/user/places/daily");
			request.addQuerystringParameter("from", from.toString());
			request.addQuerystringParameter("to", to.toString());
			Response response = send(request, credentials);
			return new PlacesResult(principal, begin, parseArray(response));
		}
	}

	private void resolveFoursquareVenues(Iterable<Event> events) {
		for (Event event : events) {
			Resource resource = event.getValue(Event.RESOURCE);
			if (resource != null) {
				FoursquareVenue venue = venues.find(resource.getTitle());
				event.setValue(Event.RESOURCE, venue.toResource());
				for (String tag : venue.getCategories()) {
					event.addValue(Event.TAG, tag);
				}
			}
		}
	}
}
