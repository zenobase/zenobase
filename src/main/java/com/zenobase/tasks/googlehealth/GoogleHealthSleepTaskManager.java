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

public class GoogleHealthSleepTaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthSleepTask> {

	@Inject
	public GoogleHealthSleepTaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthSleepTask.TYPE, credentialsManager, GoogleHealthSleepTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "sleep");
		return new GoogleHealthSleepTask(bucketId, principal, zone, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthSleepTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		paginate(credentials, "sleep:list", range.startTime(), range.endTime(), page -> {
			GoogleHealthSleepResult result = new GoogleHealthSleepResult(
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
