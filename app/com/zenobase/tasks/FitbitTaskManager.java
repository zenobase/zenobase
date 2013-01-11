package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

public class FitbitTaskManager extends FitbitTaskManagerSupport {

	@Inject
	public FitbitTaskManager(@Named("fitbit.api.key") String apiKey, @Named("fitbit.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(new FitbitApi(), apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FitbitTask.TYPE;
	}

	@Override
	public FitbitTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		FitbitTask task = super.newTask(bucketId, principal, settings).as(FitbitTask.class);
		task.setTag(Objects.firstNonNull(settings.path("tag").getTextValue(), "steps"));
		return task;
	}

	@Override
	public Command execute(Task task) {
		try {
			Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
			return execute(task.as(FitbitTask.class));
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private Command execute(FitbitTask task) {

		List<Event> events = Lists.newArrayList();
		OAuthService service = getService(task);
		LocalDate syncDate = getLastDate(task, service);
		LocalDate fromDate = getFromDate(task);
		FitbitProfileResult profile = getProfile(task, service);

		for (LocalDate date = fromDate; date.isBefore(syncDate); date = date.plusDays(1)) {
			OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
			request.addHeader("Accept-Language", profile.getDistanceLocale());
			service.signRequest(task.getToken(), request);
			Response response = request.send();
			checkResponse(task, request, response);
			events.addAll(new FitbitActivitiesResult(parseObject(response), task.getTag(), task.getPrincipal(),
				date.toDateTimeAtStartOfDay(profile.getTimezone()), profile.getDistanceUnit(), profile.getHeightUnit()).getEvents());
		}

		return createCommand(task, events, syncDate);
	}
}
