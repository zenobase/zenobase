package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.joda.time.Interval;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;

public class FitbitIntradayTaskManager extends FitbitTaskManagerSupport {

	@Inject
	public FitbitIntradayTaskManager(@Named("fitbit.api.key") String apiKey, @Named("fitbit.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new FitbitApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FitbitIntradayTask.TYPE;
	}

	@Override
	public Command execute(Task task) {
		try {
			Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
			return execute(task.as(FitbitIntradayTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(FitbitIntradayTask task) {

		List<Event> events = Lists.newArrayList();
		OAuthService service = getService(task);
		LocalDate syncDate = getLastDate(task, service);
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(task, service);
		List<Interval> sleeping = Lists.newArrayList();

		for (LocalDate date = fromDate; !date.isAfter(syncDate); date = date.plusDays(1)) {
			OAuthRequest sleepRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/sleep/date/" + date + ".json");
			service.signRequest(task.getToken(), sleepRequest);
			Response sleepResponse = sleepRequest.send();
			checkResponse(task, sleepRequest, sleepResponse);
			for (Event event : new FitbitSleepResult(parseObject(sleepResponse), task.getPrincipal(), profile.getTimezone()).getEvents()) {
				if (date.isBefore(syncDate)) {
					events.add(event);
				}
				sleeping.add(new Interval(event.getValue(Event.TIMESTAMP), event.getValue(Event.DURATION)));
			}
		}

		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest caloriesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/calories/date/" + date + "/" + date + ".json");
			service.signRequest(task.getToken(), caloriesRequest);
			Response caloriesResponse = caloriesRequest.send();
			checkResponse(task, caloriesRequest, caloriesResponse);
			events.addAll(new FitbitIntradayResult(parseObject(caloriesResponse), task.getPrincipal(), date, profile.getTimezone(), sleeping).getEvents());
		}

		return createCommand(task, events, syncDate);
	}
}
