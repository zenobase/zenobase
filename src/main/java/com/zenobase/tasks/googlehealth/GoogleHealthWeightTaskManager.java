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

public class GoogleHealthWeightTaskManager extends GoogleHealthTaskManagerSupport<GoogleHealthWeightTask> {

	@Inject
	public GoogleHealthWeightTaskManager(GoogleHealthCredentialsManager credentialsManager) {
		super(GoogleHealthWeightTask.TYPE, credentialsManager, GoogleHealthWeightTask.class);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone zone = DateTimeZone.forID(MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		DateTime begin = DateTime.parse(settings.path("marker").textValue()).withZoneRetainFields(zone);
		boolean metric = settings.path("metric").booleanValue();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "weight");
		return new GoogleHealthWeightTask(bucketId, principal, zone, metric, tag, begin.toString());
	}

	@Override
	protected List<Event> createEvents(GoogleHealthWeightTask task, OAuthCredentials credentials) {
		List<Event> events = new ArrayList<>();
		TimeRange range = rangeFor(task);
		paginate(credentials, "weight:list", range.startTime(), range.endTime(), page -> {
			GoogleHealthWeightResult result = new GoogleHealthWeightResult(
				page,
				task.getTag(),
				task.getPrincipal(),
				task.getTimezone(),
				task.isMetric()
			);
			events.addAll(result.getWeightEvents());
			return result.getNextPageToken();
		});
		// Body fat rides along on the same task so a single credential + marker set covers weight + body composition.
		paginate(credentials, "bodyFat:list", range.startTime(), range.endTime(), page -> {
			GoogleHealthWeightResult result = new GoogleHealthWeightResult(
				page,
				task.getTag(),
				task.getPrincipal(),
				task.getTimezone(),
				task.isMetric()
			);
			events.addAll(result.getBodyFatEvents());
			return result.getNextPageToken();
		});
		return events;
	}
}
