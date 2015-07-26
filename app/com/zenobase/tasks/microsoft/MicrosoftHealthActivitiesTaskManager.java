package com.zenobase.tasks.microsoft;

import java.util.List;

import javax.inject.Inject;

import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;

public class MicrosoftHealthActivitiesTaskManager extends MicrosoftHealthTaskManagerSupport<MicrosoftHealthActivitiesTask> {

	@Inject
	public MicrosoftHealthActivitiesTaskManager(MicrosoftHealthCredentialsManager credentialsManager) {
		super(MicrosoftHealthActivitiesTask.class, MicrosoftHealthActivitiesTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		boolean metric = settings.path("metric").booleanValue();
		DateTime marker = markerValue(settings.path("marker"), zone);
		return new MicrosoftHealthActivitiesTask(bucketId, principal, zone, metric, marker);
	}

	@Override
	protected List<Event> newEvents(MicrosoftHealthActivitiesTask task, DateTime begin, DateTime end, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		for (String url = "https://api.microsofthealth.net/v1/me/Activities"; url != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			if (events.isEmpty()) {
				request.addQuerystringParameter("startTime", begin.toString());
				request.addQuerystringParameter("endTime", end.toString());
				// request.addQuerystringParameter("activityIncludes", "Details,MapPoints");
				// request.addQuerystringParameter("maxPageSize", "10");
			}
			Response response = send(request, credentials);
			MicrosoftHealthActivitiesResult result = new MicrosoftHealthActivitiesResult(parse(response), task.getPrincipal(), task.getTimezone(), task.isMetric());
			events.addAll(result.getEvents());
			url = result.next();
		}
		return events;
	}
}
