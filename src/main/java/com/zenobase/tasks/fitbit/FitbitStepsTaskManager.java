package com.zenobase.tasks.fitbit;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import jakarta.inject.Inject;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;

public class FitbitStepsTaskManager extends FitbitTaskManagerSupport<FitbitStepsTask> {

	private static final Logger logger = LoggerFactory.getLogger(FitbitStepsTaskManager.class);

	@Inject
	public FitbitStepsTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitStepsTask.TYPE, FitbitStepsTask.class, credentialsManager);
	}

	@Override
	public FitbitStepsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "steps");
		boolean hourly = settings.path("hourly").booleanValue();
		return new FitbitStepsTask(bucketId, principal, marker, tag, hourly, Units.KCAL);
	}

	@Override
	protected @Nullable Command safeExecute(FitbitStepsTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = new ArrayList<>();
		LocalDate syncDate = getLastDate(DeviceType.TRACKER, credentials);
		if (syncDate == null) {
			return null;
		}
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(credentials);
		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			try {
				if (task.isHourly()) {
					OAuthRequest request = new OAuthRequest(
							Verb.GET,
							"https://api.fitbit.com/1/user/-/activities/steps/date/" + date + "/1d/15min.json");
					Response response = send(request, credentials);
					events.addAll(new FitbitIntradayStepsResult(
									parseObject(response),
									task.getTag(),
									task.getPrincipal(),
									date,
									profile.getTimezone())
							.getEvents());
				} else {
					OAuthRequest request = new OAuthRequest(
							Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
					request.addHeader("Accept-Language", profile.getDistanceLocale());
					Response response = send(request, credentials);
					events.addAll(new FitbitStepsResult(
									parseObject(response),
									task.getTag(),
									task.getPrincipal(),
									date,
									profile.getTimezone(),
									profile.getDistanceUnit(),
									profile.getHeightUnit(),
									task.getEnergyUnit(),
									task.includeBMR())
							.getEvents());
				}
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
					syncDate = date;
					break;
				}
				throw e;
			}
		}
		return createCommand(task, credentials, events, syncDate, token);
	}
}
