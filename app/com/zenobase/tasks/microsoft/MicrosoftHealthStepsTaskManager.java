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
		String marker = formatMarker(parseMarker(settings.path("marker").textValue()));
		return new MicrosoftHealthStepsTask(bucketId, principal, zone, tag, hourly, metric, marker);
	}

	@Override
	protected List<Event> newEvents(MicrosoftHealthStepsTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		DateTime from = parseMarker(task.getMarker());
		DateTime now = DateTime.now(DateTimeZone.UTC).minusMinutes(5);
		String period = task.isHourly() ? "Hourly" : "Daily";
		for (String url = "https://api.microsofthealth.net/v1/me/Summaries/" + period; url != null;) {
			OAuthRequest request = new OAuthRequest(Verb.GET, url);
			if (events.isEmpty()) {
				request.addQuerystringParameter("startTime", from.toString());
				request.addQuerystringParameter("endTime", now.toString());
			}
			Response response = send(request, credentials);
			StepsResult result = new StepsResult(parse(response), task.getPrincipal(), task.getTimezone(), task.getTag(), task.isMetric());
			events.addAll(result.getEvents());
			url = result.next();
		}
		return events;
	}
}
