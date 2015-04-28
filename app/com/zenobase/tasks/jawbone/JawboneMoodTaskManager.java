package com.zenobase.tasks.jawbone;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class JawboneMoodTaskManager extends JawboneTaskManagerSupport {

	@Inject
	public JawboneMoodTaskManager(JawboneCredentialsManager credentialsManager) {
		super(JawboneMoodTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Preconditions.checkNotNull(settings.path("tag").textValue());
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new JawboneMoodTask(bucketId, principal, tag, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(JawboneMoodTask.class), credentials);
	}

	private Command execute(JawboneMoodTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		List<Event> events = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		DateTime now = DateTime.now(DateTimeZone.UTC);
		while (from.isBefore(now)) {
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/nudge/api/v.1.1/users/@me/mood");
			request.addQuerystringParameter("date", from.toString("yyyyMMdd"));
			Response response = send(request, credentials);
			MoodResult result = new MoodResult(parseObject(response).path("data"), task.getPrincipal(), task.getTag());
			Event event = result.getEvent();
			if (event != null && event.getValue(Event.TIMESTAMP).isAfter(from)) {
				events.add(event);
			}
			from = from.plusDays(1);
		}
		return createCommand(task, credentials, events, token);
	}
}
