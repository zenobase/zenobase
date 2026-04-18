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
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;

public class FitbitBurnTaskManager extends FitbitTaskManagerSupport<FitbitBurnTask> {

	private static final Logger logger = LoggerFactory.getLogger(FitbitBurnTaskManager.class);

	@Inject
	public FitbitBurnTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitBurnTask.TYPE, FitbitBurnTask.class, credentialsManager);
	}

	@Override
	public FitbitBurnTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "burn");
		boolean hourly = settings.path("hourly").booleanValue();
		return new FitbitBurnTask(bucketId, principal, marker, tag, hourly);
	}

	@Override
	protected @Nullable Command safeExecute(FitbitBurnTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = new ArrayList<>();
		LocalDate syncDate = getLastDate(DeviceType.TRACKER, credentials);
		if (syncDate == null) {
			return null;
		}
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(credentials);

		if (task.isHourly()) {
			for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
				try {
					OAuthRequest request = new OAuthRequest(
						Verb.GET,
						"https://api.fitbit.com/1/user/-/activities/calories/date/" + date + "/1d/15min.json"
					);
					Response response = send(request, credentials);
					events.addAll(
						new FitbitBurnIntradayResult(
							parseObject(response),
							task.getTag(),
							task.getPrincipal(),
							date,
							profile.getTimezone()
						).getEvents()
					);
				} catch (InvalidStatusException e) {
					if (e.getStatus() == 429) {
						// reached rate limit
						logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
						syncDate = date;
						break;
					}
					throw e;
				}
			}
		} else {
			LocalDate toDate = syncDate.minusDays(1);
			if (!fromDate.isAfter(toDate)) {
				OAuthRequest request = new OAuthRequest(
					Verb.GET,
					"https://api.fitbit.com/1/user/-/activities/calories/date/" + fromDate + "/" + toDate + ".json"
				);
				Response response = send(request, credentials);
				events.addAll(
					new FitbitBurnResult(
						parseObject(response),
						task.getTag(),
						task.getPrincipal(),
						profile.getTimezone()
					).getEvents()
				);
			}
		}
		return createCommand(task, credentials, events, syncDate, token);
	}
}
