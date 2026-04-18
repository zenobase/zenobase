package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import jakarta.inject.Inject;
import java.util.List;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class WithingsSleepTaskManager extends WithingsTaskManagerSupport<WithingsSleepTask> {

	@Inject
	public WithingsSleepTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsSleepTask.TYPE, WithingsSleepTask.class, credentialsManager);
	}

	@Override
	public WithingsSleepTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "steps");
		DateTimeZone timezone = DateTimeZone.forID(
			MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC")
		);
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		var task = new WithingsSleepTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setTimezone(timezone);
		return task;
	}

	private static @Nullable String parseMarker(@Nullable String marker, DateTimeZone timezone) {
		return marker != null
			? LocalDateTime.parse(marker.replace("Z", "")).toDateTime(timezone).withHourOfDay(12).toString()
			: null;
	}

	@Override
	Command safeExecute(WithingsSleepTask task, OAuthCredentials credentials, Token token) {
		var result = new WithingsSleepResult(
			List.of(),
			task.getPrincipal(),
			task.getTag(),
			task.useRanges(),
			task.getTimezone()
		);
		for (
			DateTime from = Objects.requireNonNull(task.getFrom());
			from.isBefore(DateTime.now());
			from = from.plusWeeks(1)
		) {
			result.add(execute(task, credentials, from));
		}
		return createCommand(task, credentials, token, result.merge());
	}

	private List<Event> execute(WithingsSleepTask task, OAuthCredentials credentials, DateTime from) {
		OAuthRequest request = createRequest(from);
		Response response = send(request, credentials);
		var result = new WithingsSleepResult(
			parseObject(response),
			task.getPrincipal(),
			task.getTag(),
			task.useRanges(),
			task.getTimezone()
		);
		checkStatus(result, request, credentials);
		return result.getEvents();
	}

	private OAuthRequest createRequest(DateTime from) {
		var request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/v2/sleep");
		request.addQuerystringParameter("action", "get");
		request.addQuerystringParameter("startdate", toString(from));
		request.addQuerystringParameter("enddate", toString(from.plusWeeks(1)));
		return request;
	}

	private static String toString(DateTime time) {
		return Long.toString(time.getMillis() / 1000);
	}
}
