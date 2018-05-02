package com.zenobase.tasks.jawbone;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
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

public class JawboneActivitiesTaskManager extends JawboneTaskManagerSupport {

	@Inject
	public JawboneActivitiesTaskManager(JawboneCredentialsManager credentialsManager) {
		super(JawboneActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		boolean metric = settings.path("metric").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new JawboneActivitiesTask(bucketId, principal, metric, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(JawboneActivitiesTask.class), credentials);
	}

	private Command execute(JawboneActivitiesTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		for (String path = "/nudge/api/v.1.1/users/@me/workouts"; path != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			if (from != null && events.isEmpty()) {
				request.addQuerystringParameter("start_time", Long.toString(from.getMillis() / 1000));
			}
			Response response = send(request, credentials);
			WorkoutsResult result = new WorkoutsResult(parseObject(response).path("data"), task.getPrincipal(), task.isMetric());
			if (!events.addAll(result.getEvents())) {
				break;
			}
			path = result.next();
		}
		return createCommand(task, credentials, events, token);
	}
}
