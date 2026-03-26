package com.zenobase.tasks.mapmyfitness;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
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

public class MapMyFitnessSleepTaskManager extends MapMyFitnessTaskManagerSupport {

	@Inject
	public MapMyFitnessSleepTaskManager(MapMyFitnessCredentialsManager credentialsManager) {
		super(MapMyFitnessSleepTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = settings.path("marker").textValue();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "sleep");
		return new MapMyFitnessSleepTask(bucketId, principal, tag, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MapMyFitnessSleepTask.class), credentials);
	}

	private Command execute(MapMyFitnessSleepTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		UserResult user = getUser(credentials);
		String path = "/api/0.2/sleep/";
		List<Event> events = new ArrayList<>();
		DateTime from = DateTime.parse(task.getMarker());
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			request.addQuerystringParameter("user", user.getId());
			request.addQuerystringParameter("limit", "100");
			if (from != null) {
				request.addQuerystringParameter("target_start_datetime", from.toString());
			}
			Response response = send(request, credentials);
			SleepResult result = new SleepResult(parseObject(response), task.getPrincipal(), task.getTag());
			events.addAll(result.getEvents());
			path = result.getNext();
		}
		return createCommand(task, credentials, events, token);
	}
}
