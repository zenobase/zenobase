package com.zenobase.tasks.microsoft;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class MicrosoftHealthSleepTaskManager extends MicrosoftHealthTaskManagerSupport<MicrosoftHealthSleepTask> {

	@Inject
	public MicrosoftHealthSleepTaskManager(MicrosoftHealthCredentialsManager credentialsManager) {
		super(MicrosoftHealthSleepTask.class, MicrosoftHealthSleepTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "Sleep");
		DateTime marker = markerValue(settings.path("marker"), zone);
		return new MicrosoftHealthSleepTask(bucketId, principal, zone, tag, marker);
	}

	@Override
	protected List<Event> newEvents(MicrosoftHealthSleepTask task, DateTime begin, DateTime end, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		for (String url = "https://api.microsofthealth.net/v1/me/Activities"; url != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			if (events.isEmpty()) {
				request.addQuerystringParameter("startTime", begin.toString());
				request.addQuerystringParameter("endTime", end.toString());
				request.addQuerystringParameter("activityTypes", "Sleep");
				request.addQuerystringParameter("maxPageSize", "100");
			}
			Response response = send(request, credentials);
			MicrosoftHealthSleepResult result = new MicrosoftHealthSleepResult(parse(response), task.getPrincipal(), task.getTimezone(), task.getTag());
			events.addAll(result.getEvents());
			url = result.next();
		}
		return events;
	}
}
