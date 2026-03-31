package com.zenobase.tasks.mapmyfitness;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MapMyFitnessActivitiesTaskManager extends MapMyFitnessTaskManagerSupport {

	private static final Cache<String, String> TYPES =
			CacheBuilder.newBuilder().maximumSize(100).build();

	@Inject
	public MapMyFitnessActivitiesTaskManager(MapMyFitnessCredentialsManager credentialsManager) {
		super(MapMyFitnessTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
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
		List<Workout> workouts = new ArrayList<>();
		String from = task.getMarker();
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			request.addQuerystringParameter("user", user.getId());
			request.addQuerystringParameter("limit", "100");
			if (from != null) {
				request.addQuerystringParameter("started_after", from);
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

	private void resolveTypes(Iterable<Workout> workouts, OAuthCredentials credentials) {
		for (Workout workout : workouts) {
			if (workout.typeId() != null) {
				String name = resolveType(workout.typeId(), credentials);
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
			if (workout.routeId() != null) {
				OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/v7.0/route/" + workout.routeId() + "/");
				Response response = send(request, credentials);
				workout.setLocation(Objects.requireNonNull(new RouteResult(parseObject(response)).getLocation()));
			}
		}
	}

	private List<Event> getEvents(List<Workout> workouts) {
		List<Event> events = new ArrayList<>(workouts.size());
		for (Workout workout : workouts) {
			events.add(workout.event());
		}
		return events;
	}
}
