package com.zenobase.tasks.withings;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidCredentialsException;
import com.zenobase.tasks.OAuthCredentials;
import jakarta.inject.Inject;
import javax.measure.quantity.Mass;
import javax.measure.unit.Unit;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

public class WithingsWeightTaskManager extends WithingsTaskManagerSupport<WithingsWeightTask> {

	@Inject
	public WithingsWeightTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsWeightTask.TYPE, WithingsWeightTask.class, credentialsManager);
	}

	@Override
	public WithingsWeightTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "body");
		Unit<Mass> unit = MoreObjects.firstNonNull(new UnitField<Mass>("unit").getValue(settings), Units.KG);
		DateTimeZone timezone = DateTimeZone.forID(
			MoreObjects.firstNonNull(settings.path("timezone").textValue(), "UTC")
		);
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		return new WithingsWeightTask(bucketId, principal, tag, unit, timezone, marker);
	}

	private static @Nullable String parseMarker(@Nullable String marker, DateTimeZone timezone) {
		return marker != null
			? Long.toString(LocalDateTime.parse(marker.replace("Z", "")).toDateTime(timezone).getMillis() / 1000)
			: null;
	}

	@Override
	Command safeExecute(WithingsWeightTask task, OAuthCredentials credentials, Token token) {
		OAuthRequest request = createRequest(task);
		Response response = send(request, credentials);
		var result = new WithingsWeightResult(
			parseObject(response),
			task.getPrincipal(),
			task.getTag(),
			task.getUnit(),
			task.getTimezone()
		);
		if (result.getStatus() == 401) {
			throw new InvalidCredentialsException(credentials);
		}
		checkStatus(result, request, credentials);
		return createCommand(task, credentials, token, result);
	}

	private OAuthRequest createRequest(WithingsWeightTask task) {
		var request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/measure");
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("category", "1"); // actual measurements
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker());
		}
		return request;
	}
}
