package com.zenobase.tasks.fitbit;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import com.google.common.collect.Ordering;
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

public class FitbitFoodTaskManager extends FitbitTaskManagerSupport<FitbitFoodTask> {

	private static final Logger logger = LoggerFactory.getLogger(FitbitFoodTaskManager.class);

	public FitbitFoodTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitFoodTask.TYPE, FitbitFoodTask.class, credentialsManager);
	}

	@Override
	public FitbitFoodTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = MoreObjects.firstNonNull(settings.path("tag").textValue(), "steps");
		return new FitbitFoodTask(bucketId, principal, marker, tag);
	}

	@Override
	protected Command safeExecute(FitbitFoodTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = new ArrayList<>();
		FitbitProfileResult profile = getProfile(credentials);
		LocalDate today = DateTime.now(profile.getTimezone()).toLocalDate();
		LocalDate fromDate = getFromDate(task);
		for (LocalDate date = fromDate; date.isBefore(today); date = date.plusYears(1)) {
			LocalDate toDate = Objects.requireNonNull(Ordering.natural().min(today, date.plusYears(1)))
					.minusDays(1);
			OAuthRequest request = new OAuthRequest(
					Verb.GET,
					"https://api.fitbit.com/1/user/-/foods/log/caloriesIn/date/" + date + "/" + toDate + ".json");
			try {
				Response response = send(request, credentials);
				events.addAll(new FitbitFoodResult(
								parseObject(response), task.getTag(), task.getPrincipal(), profile.getTimezone())
						.getEvents());
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
					today = date;
					break;
				}
				throw e;
			}
		}
		return createCommand(task, credentials, events, today, token);
	}
}
