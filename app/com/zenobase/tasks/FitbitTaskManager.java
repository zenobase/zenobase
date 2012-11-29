package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import play.Logger;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public class FitbitTaskManager extends OAuthTaskManager {

	@Inject
	public FitbitTaskManager(@Named("fitbit.api.key") String apiKey, @Named("fitbit.api.secret") String apiSecret, @Named("oauth.hostname") String callbackUrl) {
		super(FitbitApi.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public String getType() {
		return FitbitTask.TYPE;
	}

	@Override
	public Command execute(Task task) {
		return execute(new FitbitTask(task.toJson()));
	}

	private Command execute(FitbitTask task) {

		OAuthService service = getService(task);
		List<Event> events = Lists.newArrayList();

		OAuthRequest devicesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		service.signRequest(task.getToken(), devicesRequest);
		Response devicesResponse = devicesRequest.send();
		Preconditions.checkState(devicesResponse.isSuccessful());
		LocalDate lastDate = new FitbitDevicesNode(parseArray(devicesResponse)).getLastDate();
		LocalDate fromDate = Objects.firstNonNull(task.getMarker(), LocalDate.now().minusMonths(1));

		OAuthRequest profileRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		service.signRequest(task.getToken(), profileRequest);
		Response profileResponse = profileRequest.send();
		Preconditions.checkState(profileResponse.isSuccessful());
		FitbitProfileNode profile = new FitbitProfileNode(parseObject(profileResponse));

		for (LocalDate date = fromDate.plusDays(1); !date.isAfter(lastDate); date = date.plusDays(1)) {

			OAuthRequest sleepRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/sleep/date/" + date + ".json");
			service.signRequest(task.getToken(), sleepRequest);
			Response sleepResponse = sleepRequest.send();
			Preconditions.checkState(sleepResponse.isSuccessful());
			events.addAll(new FitbitSleepNode(parseObject(sleepResponse), task.getPrincipal(), profile.getTimezone()).getEvents());

			// intraday: "https://api.fitbit.com/1/user/-/activities/calories/date/" + date + "/" + date + ".json"
			OAuthRequest activitiesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
			activitiesRequest.addHeader("Accept-Language", profile.getDistanceLocale());
			service.signRequest(task.getToken(), activitiesRequest);
			Response stepsResponse = activitiesRequest.send();
			Preconditions.checkState(stepsResponse.isSuccessful());
			events.addAll(new FitbitStepsNode(parseObject(stepsResponse), task.getPrincipal(), date.toDateTimeAtStartOfDay(profile.getTimezone()), profile.getDistanceUnit(), profile.getHeightUnit()).getEvents());
		}

		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "imported events from fitbit", "removed events imported from fitbit");
		FitbitTask to = task.copy();
		to.setUpdated(new DateTime(DateTimeZone.UTC));
		to.setMarker(lastDate);
		command.add(new UpdateTaskCommand(task.getPrincipal(), task.getBucketId(), task, to));

		for (Event event : events) {
			Logger.info("import: " + event.toJson());
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}

		return command;
	}
}
