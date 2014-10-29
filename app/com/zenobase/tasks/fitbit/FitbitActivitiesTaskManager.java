package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import play.Logger;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class FitbitActivitiesTaskManager extends FitbitTaskManagerSupport<FitbitActivitiesTask> {

	@Inject
	public FitbitActivitiesTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitActivitiesTask.TYPE, FitbitActivitiesTask.class, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		return new FitbitActivitiesTask(bucketId, principal, marker);
	}

	@Override
	protected Command safeExecute(FitbitActivitiesTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		FitbitProfileResult profile = getProfile(task, credentials);
		LocalDate today = new DateTime(profile.getTimezone()).toLocalDate();
		LocalDate fromDate = getFromDate(task);
		for (LocalDate date = fromDate; date.isBefore(today); date = date.plusDays(1)) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
			try {
				Response response = send(request, credentials);
				events.addAll(new FitbitActivitiesResult(parseObject(response), task.getPrincipal(), profile.getTimezone(), profile.getDistanceUnit()).getEvents());
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					Logger.warn("Hit rate limit and couldn't complete task: {}", task.getId());
					today = date;
					break;
				}
				throw e;
			}
		}
		// TODO get last timestamp
		return createCommand(task, events, today);
	}
}
