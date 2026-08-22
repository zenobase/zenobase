package com.zenobase.tasks.googlehealth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

public class GoogleHealthActivitiesTaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthActivitiesTask> {

	@Inject
	public GoogleHealthActivitiesTaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthActivitiesTask.TYPE, credentialsManager, GoogleHealthActivitiesTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		boolean metric = settings.path("metric").booleanValue();
		return new GoogleHealthActivitiesTask(bucketId, principal, zone, metric, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthActivitiesTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		paginate(credentials, "exercise:list", range.startTime(), range.endTime(), page -> {
			GoogleHealthActivitiesResult result = new GoogleHealthActivitiesResult(
				page,
				task.getPrincipal(),
				task.getTimezone(),
				task.isMetric()
			);
			events.addAll(result.getEvents());
			return result.getNextPageToken();
		});
		return events;
	}
}
