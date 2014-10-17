package com.zenobase.tasks.fitbit;

import java.util.List;

import javax.inject.Inject;

import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.common.Units;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class FitbitStepsTaskManager extends FitbitTaskManagerSupport {

	@Inject
	public FitbitStepsTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitStepsTask.TYPE, credentialsManager);
	}

	@Override
	public FitbitStepsTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		return new FitbitStepsTask(bucketId, principal, marker, tag, Units.KCAL);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(FitbitStepsTask.class), credentials);
	}

	private Command execute(FitbitStepsTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		LocalDate syncDate = getLastDate(DeviceType.TRACKER, task, credentials);
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(task, credentials);
		for (LocalDate date = fromDate; syncDate != null && date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
			request.addHeader("Accept-Language", profile.getDistanceLocale());
			try {
				Response response = send(request, credentials);
				events.addAll(new FitbitStepsResult(parseObject(response), task.getTag(), task.getPrincipal(),
					date, profile.getTimezone(), profile.getDistanceUnit(), profile.getHeightUnit(), task.getEnergyUnit()).getEvents());
			} catch (InvalidStatusException e) {
				if (e.getStatus() == 429) { // reached rate limit
					syncDate = date;
					break;
				}
				throw e;
			}
		}
		return createCommand(task, events, syncDate);
	}
}
