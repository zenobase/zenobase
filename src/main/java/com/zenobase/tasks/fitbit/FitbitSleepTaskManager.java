package com.zenobase.tasks.fitbit;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.joda.time.LocalDate;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class FitbitSleepTaskManager extends FitbitTaskManagerSupport<FitbitSleepTask> {

	private static final Logger logger = LoggerFactory.getLogger(FitbitSleepTaskManager.class);

	@Inject
	public FitbitSleepTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitSleepTask.TYPE, FitbitSleepTask.class, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "sleep");
		return new FitbitSleepTask(bucketId, principal, marker, tag);
	}

	@Override
	protected @Nullable Command safeExecute(FitbitSleepTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = new ArrayList<>();
		LocalDate syncDate = getLastDate(DeviceType.TRACKER, credentials);
		if (syncDate == null) {
			return null;
		}
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(credentials);
		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest request = new OAuthRequest(
				Verb.GET,
				"https://api.fitbit.com/1/user/-/sleep/date/" + date + ".json"
			);
			try {
				Response response = send(request, credentials);
				events.addAll(
					new FitbitSleepResult(
						parseObject(response),
						task.getTag(),
						task.getPrincipal(),
						task.useRanges(),
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
		return createCommand(task, credentials, events, syncDate, token);
	}
}
