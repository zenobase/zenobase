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
import com.zenobase.models.Event;
import com.zenobase.models.Identity;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.Task;

public class FitbitTaskManager extends FitbitTaskManagerSupport {

	@Inject
	public FitbitTaskManager(FitbitCredentialsManager credentialsManager) {
		super(FitbitTask.TYPE, credentialsManager);
	}

	@Override
	public FitbitTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		String marker = parseMarker(settings.path("marker").textValue()).toString();
		String tag = Objects.firstNonNull(settings.path("tag").textValue(), "steps");
		return new FitbitTask(bucketId, principal, marker, tag);
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		return execute(task.as(FitbitTask.class), credentials);
	}

	private Command execute(FitbitTask task, OAuthCredentials credentials) {
		List<Event> events = Lists.newArrayList();
		LocalDate syncDate = getLastDate(task, credentials);
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(task, credentials);
		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
			request.addHeader("Accept-Language", profile.getDistanceLocale());
			Response response = send(request, credentials);
			events.addAll(new FitbitActivitiesResult(parseObject(response), task.getTag(), task.getPrincipal(),
				date.toDateTimeAtStartOfDay(profile.getTimezone()), profile.getDistanceUnit(), profile.getHeightUnit()).getEvents());
		}
		return createCommand(task, events, syncDate);
	}
}
