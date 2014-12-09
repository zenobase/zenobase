package com.zenobase.tasks.misfit;

import java.util.List;

import javax.inject.Inject;

import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MisfitActivitiesTaskManager extends MisfitTaskManagerSupport {

	@Inject
	public MisfitActivitiesTaskManager(MisfitCredentialsManager credentialsManager) {
		super(MisfitActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new MisfitActivitiesTask(bucketId, principal, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MisfitActivitiesTask.class), credentials);
	}

	private Command execute(MisfitActivitiesTask task, OAuthCredentials credentials) {
		DateTime begin = task.getBegin();
		DateTime now = DateTime.now(begin.getZone());
		List<Event> events = Lists.newArrayList();
		while (begin.isBefore(now)) {
			DateTime end = begin.plusWeeks(4);
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/activity/sessions");
			request.addQuerystringParameter("start_date", begin.toLocalDate().toString());
			request.addQuerystringParameter("end_date", end.toLocalDate().toString());
			Response response = send(request, credentials);
			events.addAll(new MisfitActivitiesResult(parseObject(response), task.getPrincipal(), begin).getEvents());
			begin = end;
		}
		return createCommand(task, credentials, events);
	}
}
