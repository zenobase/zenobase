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

public class GoogleHealthCardioTaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthCardioTask> {

	@Inject
	public GoogleHealthCardioTaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthCardioTask.TYPE, credentialsManager, GoogleHealthCardioTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "heart rate");
		boolean hourly = settings.path("hourly").booleanValue();
		return new GoogleHealthCardioTask(bucketId, principal, zone, tag, hourly, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthCardioTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		// Hourly tasks pull the raw heart-rate series; daily tasks pull the pre-aggregated daily resting HR.
		String resource = task.isHourly() ? "heartRate:list" : "dailyRestingHeartRate:list";
		paginate(credentials, resource, range.startTime(), range.endTime(), page -> {
			GoogleHealthCardioResult result = new GoogleHealthCardioResult(
				page,
				task.getTag(),
				task.getPrincipal(),
				task.getTimezone()
			);
			events.addAll(result.getEvents());
			return result.getNextPageToken();
		});
		return events;
	}
}
