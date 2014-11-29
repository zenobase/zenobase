package com.zenobase.tasks.mapmyfitness;

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
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.RateLimiter;

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

public class MapMyFitnessTaskManager extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(1);
	private static final String HOST = "https://oauth2-api.mapmyapi.com";
	private static final Cache<String, String> TYPES = CacheBuilder.newBuilder().maximumSize(100).build();

	@Inject
	public MapMyFitnessTaskManager(MapMyFitnessCredentialsManager credentialsManager) {
		super(MapMyFitnessTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = settings.path("marker").textValue();
		return new MapMyFitnessTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MapMyFitnessTask.class), credentials);
	}

	private Command execute(MapMyFitnessTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		UserResult user = getUser(credentials);
		String path = "/v7.0/workout/";
		List<Workout> workouts = Lists.newArrayList();
		String from = task.getMarker();
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			request.addQuerystringParameter("user", user.getId());
			request.addQuerystringParameter("limit", "100");
			if (from != null) {
				request.addQuerystringParameter("started_after", from.toString());
			}
			Response response = send(request, credentials);
			WorkoutsResult result = new WorkoutsResult(parseObject(response), task.getPrincipal(), user.isImperial());
			workouts.addAll(result.getWorkouts());
			path = result.getNext();
		}
		resolveTypes(workouts, credentials);
		resolveRoutes(workouts, credentials);
		return createCommand(task, credentials, getEvents(workouts), token);
	}

	private UserResult getUser(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/v7.0/user/self/");
		Response response = send(request, credentials);
		return new UserResult(parseObject(response));
	}

	private void resolveTypes(Iterable<Workout> workouts, OAuthCredentials credentials) {
		for (Workout workout : workouts) {
			if (workout.getTypeId() != null) {
				String name = resolveType(workout.getTypeId(), credentials);
				if (name != null) {
					workout.addTag(name);
				}
			}
		}
	}

	private String resolveType(String typeId, OAuthCredentials credentials) {
		String name = TYPES.getIfPresent(typeId);
		if (name == null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/v7.0/activity_type/" + typeId + "/");
			Response response = send(request, credentials);
			name = new TypeResult(parseObject(response)).getName();
			if (name != null) {
				TYPES.put(typeId, name);
			}
		}
		return name;
	}

	private void resolveRoutes(Iterable<Workout> workouts, OAuthCredentials credentials) {
		for (Workout workout : workouts) {
			if (workout.getRouteId() != null) {
				OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/v7.0/route/" + workout.getRouteId() + "/");
				Response response = send(request, credentials);
				workout.setLocation(new RouteResult(parseObject(response)).getLocation());
			}
		}
	}

	private List<Event> getEvents(List<Workout> workouts) {
		List<Event> events = Lists.newArrayListWithExpectedSize(workouts.size());
		for (Workout workout : workouts) {
			events.add(workout.getEvent());
		}
		return events;
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

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire(1);
		return super.send(request, credentials);
	}

	private Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, Token expiredToken) {
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
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}
		return command;
	}
}
