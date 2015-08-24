package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;
import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import play.Logger;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;

public class FitbitFoodTaskManager extends FitbitTaskManagerSupport<FitbitFoodTask> {

	@Inject
	public FitbitFoodTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitFoodTask.TYPE, FitbitFoodTask.class, credentialsManager);
	}

	@Override
	public FitbitFoodTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		return new FitbitFoodTask(bucketId, principal, marker, tag);
	}

	@Override
	protected Command safeExecute(FitbitFoodTask task, OAuthCredentials credentials, Token token) {
		List<Event> events = Lists.newArrayList();
		FitbitProfileResult profile = getProfile(task, credentials);
		LocalDate today = new DateTime(profile.getTimezone()).toLocalDate();
		LocalDate fromDate = getFromDate(task);
		for (LocalDate date = fromDate; date.isBefore(today); date = date.plusYears(1)) {
			LocalDate toDate = Ordering.natural().min(today, date.plusYears(1)).minusDays(1);
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/foods/log/caloriesIn/date/" + date + "/" + toDate + ".json");
			try {
				Response response = send(request, credentials);
				events.addAll(new FitbitFoodResult(parseObject(response), task.getTag(), task.getPrincipal(), profile.getTimezone()).getEvents());
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					Logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
					today = date;
					break;
				}
				throw e;
			}
		}
		return createCommand(task, credentials, events, today, token);
	}
}
