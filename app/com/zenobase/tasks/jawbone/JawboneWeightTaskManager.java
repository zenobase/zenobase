package com.zenobase.tasks.jawbone;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
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

public class JawboneWeightTaskManager extends JawboneTaskManagerSupport {

	@Inject
	public JawboneWeightTaskManager(JawboneCredentialsManager credentialsManager) {
		super(JawboneWeightTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Preconditions.checkNotNull(settings.path("tag").textValue());
		boolean metric = settings.path("metric").booleanValue();
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new JawboneWeightTask(bucketId, principal, tag, metric, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(JawboneWeightTask.class), credentials);
	}

	private Command execute(JawboneWeightTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		for (String path = "/nudge/api/v.1.1/users/@me/body_events"; path != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + path);
			if (from != null && events.isEmpty()) {
				request.addQuerystringParameter("start_time", Long.toString(from.getMillis() / 1000));
			}
			Response response = send(request, credentials);
			WeightResult result = new WeightResult(parseObject(response).path("data"), task.getPrincipal(), task.getTag(), task.isMetric());
			if (!events.addAll(result.getEvents())) {
				break;
			}
			path = result.next();
		}
		return createCommand(task, credentials, events, token);
	}
}
