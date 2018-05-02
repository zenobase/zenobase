package com.zenobase.tasks.mapmyfitness;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
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

public class MapMyFitnessWeightTaskManager extends MapMyFitnessTaskManagerSupport {

	@Inject
	public MapMyFitnessWeightTaskManager(MapMyFitnessCredentialsManager credentialsManager) {
		super(MapMyFitnessWeightTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "body");
		return new MapMyFitnessWeightTask(bucketId, principal, tag, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MapMyFitnessWeightTask.class), credentials);
	}

	private Command execute(MapMyFitnessWeightTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		UserResult user = getUser(credentials);
		String path = "/api/0.1/bodymass/";
		List<Event> events = Lists.newArrayList();
		String from = task.getMarker();
		while (path != null) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			request.addQuerystringParameter("user", user.getId());
			request.addQuerystringParameter("limit", "100");
			if (from != null) {
				request.addQuerystringParameter("target_start_datetime", from.toString());
			}
			Response response = send(request, credentials);
			WeightResult result = new WeightResult(parseObject(response), task.getPrincipal(), task.getTag(), user.isImperial());
			events.addAll(result.getEvents());
			path = result.getNext();
		}
		return createCommand(task, credentials, events, token);
	}
}
