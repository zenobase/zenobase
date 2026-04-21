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

public class GoogleHealthSpo2TaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthSpo2Task> {

	@Inject
	public GoogleHealthSpo2TaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthSpo2Task.TYPE, credentialsManager, GoogleHealthSpo2Task.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "spo2");
		return new GoogleHealthSpo2Task(bucketId, principal, zone, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthSpo2Task task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		paginate(credentials, "dailyOxygenSaturation:list", range.startTime(), range.endTime(), page -> {
			GoogleHealthSpo2Result result = new GoogleHealthSpo2Result(
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
