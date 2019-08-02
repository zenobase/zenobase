package com.zenobase.tasks.oura;

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

public class OuraSleepTaskManager extends OuraTaskManagerSupport {

	@Inject
	public OuraSleepTaskManager(OuraCredentialsManager credentialsManager) {
		super(OuraSleepTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "sleep");
		return new OuraSleepTask(bucketId, principal, marker, tag);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(OuraSleepTask.class), credentials);
	}

	private Command execute(OuraSleepTask task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired()) {
			reauthorize(credentials);
		}
		DateTime begin = task.getBegin();
		DateTime end = DateTime.now(begin.getZone()).plusDays(1);
		List<Event> events = Lists.newArrayList();
		OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/v1/sleep");
		request.addQuerystringParameter("start", begin.toLocalDate().toString());
		request.addQuerystringParameter("end", end.toLocalDate().toString());
		Response response = send(request, credentials);
		events.addAll(new OuraSleepResult(parseObject(response), task.getPrincipal(), task.getTag()).getEvents());
		return createCommand(task, credentials, events, token);
	}
}
