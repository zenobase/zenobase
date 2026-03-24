package com.zenobase.tasks.oura;

import java.util.List;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class OuraStepsTaskManager extends OuraTaskManagerSupport {

	@Inject
	public OuraStepsTaskManager(OuraCredentialsManager credentialsManager) {
		super(OuraStepsTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "steps");
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		return new OuraStepsTask(bucketId, principal, marker, tag, zone);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(OuraStepsTask.class), credentials);
	}

	private Command execute(OuraStepsTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime begin = task.getBegin();
		DateTime end = DateTime.now(begin.getZone()).plusDays(1);
		List<Event> events = Lists.newArrayList();
		OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/v2/usercollection/daily_activity");
		request.addQuerystringParameter("start_date", begin.toLocalDate().toString());
		request.addQuerystringParameter("end_date", end.toLocalDate().toString());
		Response response = send(request, credentials);
		events.addAll(new OuraStepsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getTimezone()).getEvents());
		return createCommand(task, credentials, events, token);
	}
}
