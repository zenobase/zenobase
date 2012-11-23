package com.zenobase.tasks;

import java.util.List;

import org.elasticsearch.common.collect.Lists;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.zenobase.commands.Command;
import com.zenobase.models.Event;

public class FitbitTaskManager extends OAuthTaskManager<FitbitTask> {

	public FitbitTaskManager(String apiKey, String apiSecret, String callbackUrl) {
		super(FitbitApi.class, apiKey, apiSecret, callbackUrl);
	}

	@Override
	public Command execute(FitbitTask task) {

		OAuthService service = getService(task);
		List<Event> events = Lists.newArrayList();

		OAuthRequest profileRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		service.signRequest(task.getToken(), profileRequest);
		Response profileResponse = profileRequest.send();
		FitbitProfileNode profile = new FitbitProfileNode(parseObject(profileResponse));

		OAuthRequest devicesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		service.signRequest(task.getToken(), devicesRequest);
		Response devicesResponse = devicesRequest.send();
		LocalDate lastDate = new FitbitDevicesNode(parseArray(devicesResponse)).getLastDate();
		System.out.println("marker: " + lastDate);
		// TODO task.setMarker();

		OAuthRequest sleepRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/sleep/date/" + lastDate + ".json");
		service.signRequest(task.getToken(), sleepRequest);
		Response sleepResponse = sleepRequest.send();
		events.addAll(new FitbitSleepNode(parseObject(sleepResponse), profile.getTimezone()).getEvents());

		// intraday: "https://api.fitbit.com/1/user/-/activities/calories/date/" + date + "/" + date + ".json"
		OAuthRequest stepsRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + lastDate + ".json");
		stepsRequest.addHeader("Accept-Language", profile.getDistanceLocale());
		service.signRequest(task.getToken(), stepsRequest);
		Response stepsResponse = stepsRequest.send();
		events.addAll(new FitbitStepsNode(parseObject(stepsResponse), profile.getDistanceUnit(), profile.getHeightUnit()).getEvents());

		for (Event event : events) {
			System.out.println("event: " + event.toJson());
		}

		return null; // TODO
	}
}
