package com.zenobase.tasks.fitbit;

import java.util.List;

import jakarta.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
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
import com.zenobase.tasks.Task;

public class FitbitWeightTaskManager extends FitbitTaskManagerSupport<FitbitWeightTask> {

	private static final Logger logger = LoggerFactory.getLogger(FitbitWeightTaskManager.class);

	@Inject
	public FitbitWeightTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitWeightTask.TYPE, FitbitWeightTask.class, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "body");
		return new FitbitWeightTask(bucketId, principal, marker, tag);
	}

	@Override
	protected Command safeExecute(FitbitWeightTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = Lists.newArrayList();
		FitbitProfileResult profile = getProfile(credentials);
		LocalDate syncDate = getLastDate(DeviceType.SCALE, credentials);
		if (syncDate == null) {
			syncDate = DateTime.now(profile.getTimezone()).toLocalDate();
		}
		LocalDate fromDate = getFromDate(task);
		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/body/date/" + date + ".json");
			request.addHeader("Accept-Language", profile.getWeightLocale());
			try {
				Response response = send(request, credentials);
				events.addAll(new FitbitWeightResult(parseObject(response), task.getTag(), task.getPrincipal(), date, profile.getTimezone(), profile.getWeightUnit()).getEvents());
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
