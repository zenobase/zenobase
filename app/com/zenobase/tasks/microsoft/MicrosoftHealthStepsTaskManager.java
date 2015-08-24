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

public class MicrosoftHealthStepsTaskManager extends MicrosoftHealthTaskManagerSupport<MicrosoftHealthStepsTask> {

	@Inject
	public MicrosoftHealthStepsTaskManager(MicrosoftHealthCredentialsManager credentialsManager) {
		super(MicrosoftHealthStepsTask.class, MicrosoftHealthStepsTask.TYPE, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "Steps");
		DateTimeZone zone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		boolean hourly = settings.path("hourly").booleanValue();
		boolean metric = settings.path("metric").booleanValue();
		DateTime marker = markerValue(settings.path("marker"), zone);
		return new MicrosoftHealthStepsTask(bucketId, principal, zone, tag, hourly, metric, marker);
	}

	@Override
	protected List<Event> newEvents(MicrosoftHealthStepsTask task, DateTime begin, DateTime end, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		String period = task.isHourly() ? "Hourly" : "Daily";
		for (String url = "https://api.microsofthealth.net/v1/me/Summaries/" + period; url != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			if (events.isEmpty()) {
				request.addQuerystringParameter("startTime", begin.toString());
				request.addQuerystringParameter("endTime", end.toString());
			}
			Response response = send(request, credentials);
			MicrosoftHealthStepsResult result = new MicrosoftHealthStepsResult(parse(response), task.getPrincipal(), task.getTimezone(), task.getTag(), task.isMetric());
			events.addAll(result.getEvents());
			url = result.next();
		}
		if (!events.isEmpty()) { // could be incomplete
			events.remove(0);
		}
		return events;
	}
}
