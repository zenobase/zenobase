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

public class GoogleHealthFoodTaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthFoodTask> {

	@Inject
	public GoogleHealthFoodTaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthFoodTask.TYPE, credentialsManager, GoogleHealthFoodTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "food");
		return new GoogleHealthFoodTask(bucketId, principal, zone, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthFoodTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		paginate(credentials, "nutrition:list", range.startTime(), range.endTime(), page -> {
			GoogleHealthFoodResult result = new GoogleHealthFoodResult(
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
