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

public class GoogleHealthStepsTaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthStepsTask> {

	@Inject
	public GoogleHealthStepsTaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthStepsTask.TYPE, credentialsManager, GoogleHealthStepsTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "steps");
		boolean hourly = settings.path("hourly").booleanValue();
		return new GoogleHealthStepsTask(bucketId, principal, zone, tag, hourly, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthStepsTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		// :list returns sub-day points (hourly); :dailyRollup returns one point per day.
		String resource = task.isHourly() ? "steps:list" : "steps:dailyRollup";
		paginate(credentials, resource, range.startTime(), range.endTime(), page -> {
			GoogleHealthStepsResult result = new GoogleHealthStepsResult(
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
