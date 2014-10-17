package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;
import com.google.common.collect.Ordering;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class FitbitFoodTaskManager extends FitbitTaskManagerSupport {

	@Inject
	public FitbitFoodTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitFoodTask.TYPE, credentialsManager);
	}

	@Override
	public FitbitFoodTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		return new FitbitFoodTask(bucketId, principal, marker, tag);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(FitbitFoodTask.class), credentials);
	}

	private Command execute(FitbitFoodTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		FitbitProfileResult profile = getProfile(task, credentials);
		LocalDate today = new DateTime(profile.getTimezone()).toLocalDate();
		LocalDate fromDate = getFromDate(task);
		for (LocalDate date = fromDate; today != null && date.isBefore(today); date = date.plusYears(1)) {
			LocalDate toDate = Ordering.natural().min(today, date.plusYears(1)).minusDays(1);
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/foods/log/caloriesIn/date/" + date + "/" + toDate + ".json");
			try {
				Response response = send(request, credentials);
				events.addAll(new FitbitFoodResult(parseObject(response), task.getTag(), task.getPrincipal(), profile.getTimezone()).getEvents());
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					today = date;
					break;
				}
				throw e;
			}
		}
		return createCommand(task, events, today);
	}
}
