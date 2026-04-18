package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import jakarta.inject.Inject;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;

public class WithingsCardioTaskManager extends WithingsTaskManagerSupport<WithingsCardioTask> {

	@Inject
	public WithingsCardioTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsCardioTask.TYPE, WithingsCardioTask.class, credentialsManager);
	}

	@Override
	public WithingsCardioTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		DateTimeZone timezone = DateTimeZone.forID(
			MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC")
		);
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "heart rate");
		var task = new WithingsCardioTask(bucketId, principal, marker);
		task.setTag(tag);
		task.setTimezone(timezone);
		return task;
	}

	private static @Nullable String parseMarker(@Nullable String marker, DateTimeZone timezone) {
		return marker != null
			? Long.toString(LocalDateTime.parse(marker.replace("Z", "")).toDateTime(timezone).getMillis() / 1000)
			: null;
	}

	@Override
	Command safeExecute(WithingsCardioTask task, OAuthCredentials credentials, Token token) {
		OAuthRequest request = createRequest(task);
		Response response = send(request, credentials);
		var result = new WithingsCardioResult(
			parseObject(response),
			task.getPrincipal(),
			task.getTag(),
			task.getTimezone()
		);
		checkStatus(result, request, credentials);
		return createCommand(task, credentials, token, result);
	}

	private OAuthRequest createRequest(WithingsCardioTask task) {
		var request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/measure");
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("category", "1"); // actual measurements
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker());
		}
		return request;
	}
}
