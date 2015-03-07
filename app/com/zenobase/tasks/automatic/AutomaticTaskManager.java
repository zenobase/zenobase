package com.zenobase.tasks.automatic;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.collect.Lists;

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

public class AutomaticTaskManager extends OAuthTaskManager {

	@Inject
	public AutomaticTaskManager(AutomaticCredentialsManager credentialsManager) {
		super(AutomaticTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = settings.path("tag").textValue();
		boolean metric = settings.path("metric").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new AutomaticTask(bucketId, principal, tag, metric, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(AutomaticTask.class), credentials);
	}

	private Command execute(AutomaticTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Trip> trips = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		TRIPS:
		for (int i = 0; i < 100; ++i) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.automatic.com/v1/trips");
			request.addQuerystringParameter("per_page", "10");
			request.addQuerystringParameter("page", Integer.toString(i + 1));
			Response response = send(request, credentials);
			List<Trip> add = new TripsResult(parseArray(response), task.getPrincipal(), task.getTag(), task.isMetric()).getTrips();
			if (add.isEmpty()) {
				break TRIPS;
			}
			for (Trip trip : add) {
				if (from != null && !trip.isAfter(from)) {
					break TRIPS;
				}
				trips.add(trip);
			}
		}
		resolveVehicles(trips, credentials);
		return createCommand(task, credentials, getEvents(trips), token);
	}


	private void resolveVehicles(Iterable<Trip> trips, final OAuthCredentials credentials) {
		LoadingCache<String, String> vehicles = CacheBuilder.newBuilder().maximumSize(10).build(new CacheLoader<String, String>() {
			@Override
			public String load(String key) {
				OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.automatic.com/v1/vehicles/" + key);
				Response response = send(request, credentials);
				return new VehicleResult(parseObject(response)).getDisplayName();
			}
		});
		for (Trip trip : trips) {
			if (trip.getVehicleId() != null) {
				String name = vehicles.getUnchecked(trip.getVehicleId());
				if (name != null) {
					trip.addTag(name);
				}
			}
		}
	}

	private List<Event> getEvents(List<Trip> trips) {
		List<Event> events = Lists.newArrayListWithExpectedSize(trips.size());
		for (Trip trip : trips) {
			events.add(trip.getEvent());
		}
		return events;
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

	private Command createCommand(AutomaticTask task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), events.isEmpty() ? task.getMarker() : getMarker(events).toString())
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
