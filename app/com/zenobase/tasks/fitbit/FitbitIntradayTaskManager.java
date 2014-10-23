package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class FitbitIntradayTaskManager extends FitbitTaskManagerSupport<FitbitIntradayTask> {

	@Inject
	public FitbitIntradayTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitIntradayTask.TYPE, FitbitIntradayTask.class, credentialsManager);
	}

	@Override
	public Task newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		return new FitbitIntradayTask(bucketId, principal, marker);
	}

	@Override
	protected Command safeExecute(FitbitIntradayTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		LocalDate syncDate = getLastDate(DeviceType.TRACKER, task, credentials);
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(task, credentials);
		List<Interval> sleeping = Lists.newArrayList();
		for (LocalDate date = fromDate; syncDate != null && !date.isAfter(syncDate); date = date.plusDays(1)) {
			OAuthRequest sleepRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/sleep/date/" + date + ".json");
			Response sleepResponse = send(sleepRequest, credentials);
			for (Event event : new FitbitSleepResult(parseObject(sleepResponse), "sleeping", task.getPrincipal(), false, profile.getTimezone()).getEvents()) {
				if (date.isBefore(syncDate)) {
					events.add(event);
				}
				sleeping.add(new Interval(event.getValue(Event.TIMESTAMP), event.getValue(Event.DURATION)));
			}
		}
		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest caloriesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/calories/date/" + date + "/" + date + ".json");
			Response caloriesResponse = send(caloriesRequest, credentials);
			events.addAll(new FitbitIntradayResult(parseObject(caloriesResponse), task.getPrincipal(), date, profile.getTimezone(), sleeping).getEvents());
		}
		return createCommand(task, events, syncDate);
	}
}
