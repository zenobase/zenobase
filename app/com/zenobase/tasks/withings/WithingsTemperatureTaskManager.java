package com.zenobase.tasks.withings;

import javax.inject.Inject;
import javax.measure.quantity.Temperature;
import javax.measure.unit.Unit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDateTime;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;

import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.json.UnitField;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidCredentialsException;
import com.zenobase.tasks.OAuthCredentials;

public class WithingsTemperatureTaskManager extends WithingsTaskManagerSupport<WithingsTemperatureTask> {

	@Inject
	public WithingsTemperatureTaskManager(WithingsCredentialsManager credentialsManager) {
		super(WithingsTemperatureTask.TYPE, WithingsTemperatureTask.class, credentialsManager);
	}

	@Override
	public WithingsTemperatureTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "body");
		Unit<Temperature> unit = Objects.firstNonNull(new UnitField<Temperature>("unit").getValue(settings), Units.C);
		DateTimeZone timezone = DateTimeZone.forID(Objects.firstNonNull(settings.path("timezone").textValue(), "UTC"));
		String marker = parseMarker(settings.path("marker").textValue(), timezone);
		return new WithingsTemperatureTask(bucketId, principal, tag, unit, timezone, marker);
	}

	private static String parseMarker(String marker, DateTimeZone timezone) {
		return marker != null ? Long.toString(LocalDateTime.parse(marker.replaceAll("Z", "")).toDateTime(timezone).getMillis() / 1000) : null;
	}

	@Override
	Command safeExecute(WithingsTemperatureTask task, OAuthCredentials credentials, Token token) {
		OAuthRequest request = createRequest(task);
		Response response = send(request, credentials);
		WithingsTemperatureResult result = new WithingsTemperatureResult(parseObject(response), task.getPrincipal(), task.getTag(), task.getUnit(), task.getTimezone());
		if (result.getStatus() == 401) {
			throw new InvalidCredentialsException(credentials);
		}
		checkStatus(result, request, credentials);
		return createCommand(task, credentials, token, result);
	}

	private OAuthRequest createRequest(WithingsTemperatureTask task) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://wbsapi.withings.net/measure");
		request.addQuerystringParameter("action", "getmeas");
		request.addQuerystringParameter("category", "1"); // actual measurements
		request.addQuerystringParameter("meastype", "71"); // body temperature
		if (task.getMarker() != null) {
			request.addQuerystringParameter("lastupdate", task.getMarker());
		}
		return request;
	}
}
