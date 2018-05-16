package com.zenobase.tasks.misfit;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MisfitStepsTaskManager extends MisfitTaskManagerSupport {

	@Inject
	public MisfitStepsTaskManager(MisfitCredentialsManager credentialsManager) {
		super(MisfitStepsTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Strings.emptyToNull(settings.path("tag").textValue());
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = DateTime.parse(settings.path("marker").textValue()).toString();
		return new MisfitStepsTask(bucketId, principal, tag, zone, marker);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(MisfitStepsTask.class), credentials);
	}

	private Command execute(MisfitStepsTask task, OAuthCredentials credentials) {
		LocalDate begin = task.getBegin().toLocalDate().plusDays(1);
		LocalDate today = LocalDate.now(task.getBegin().getZone());
		List<Event> events = Lists.newArrayList();
		while (!begin.isAfter(today)) {
			LocalDate end = begin.plusWeeks(4);
			OAuthRequest request = new OAuthRequest(Verb.GET, HOST + "/activity/summary");
			request.addQuerystringParameter("start_date", begin.toString());
			request.addQuerystringParameter("end_date", end.toString());
			request.addQuerystringParameter("detail", "true");
			Response response = send(request, credentials);
			events.addAll(new MisfitStepsResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getTimezone()).getEvents());
			begin = end;
		}
		if (!events.isEmpty()) {
			events.remove(events.size() - 1); // the last event could be incomplete
		}
		return createCommand(task, credentials, events);
	}
}
