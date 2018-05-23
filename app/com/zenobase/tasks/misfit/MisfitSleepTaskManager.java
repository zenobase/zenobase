package com.zenobase.tasks.misfit;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MisfitSleepTaskManager extends MisfitTaskManagerSupport {

	@Inject
	public MisfitSleepTaskManager(MisfitCredentialsManager credentialsManager) {
		super(MisfitSleepTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new MisfitSleepTask(bucketId, principal, tag, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MisfitSleepTask.class), credentials);
	}

	private Command execute(MisfitSleepTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		LocalDate begin = task.getBegin().toLocalDate().plusDays(1);
		LocalDate today = LocalDate.now(task.getBegin().getZone());
		while (!begin.isAfter(today)) {
			LocalDate end = begin.plusWeeks(4);
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/activity/sleeps");
			request.addQuerystringParameter("start_date", begin.toString());
			request.addQuerystringParameter("end_date", end.toString());
			Response response = send(request, credentials);
			events.addAll(new MisfitSleepResult(parseObject(response), task.getPrincipal(), task.getTag()).getEvents());
			begin = end;
		}
		return createCommand(task, events);
	}
}
