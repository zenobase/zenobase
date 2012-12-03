package com.zenobase.tasks;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import com.google.common.base.Objects;
import com.google.common.base.Preconditions;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.models.Identity;

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
	public FitbitTask newTask(String bucketId, Identity principal, ObjectNode settings) {
		FitbitTask task = super.newTask(bucketId, principal, settings).as(FitbitTask.class);
		task.setTag(Objects.firstNonNull(settings.path("tag").getTextValue(), "steps"));
		return task;
	}
	@Override
	public Command execute(Task task) {
		Preconditions.checkState(task.isEnabled(), "Task is not enabled: %s", task.getId());
		return execute(task.as(FitbitTask.class));
	}

	private Command execute(FitbitTask task) {

		OAuthService service = getService(task);
		List<Event> events = Lists.newArrayList();

		OAuthRequest devicesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		service.signRequest(task.getToken(), devicesRequest);
		Response devicesResponse = devicesRequest.send();
		Preconditions.checkState(devicesResponse.isSuccessful(), "Failed to get devices for task <%s>", task.getId());
		LocalDate lastDate = new FitbitDevicesResult(parseArray(devicesResponse)).getLastDate();
		LocalDate fromDate = task.getMarker() != null ? LocalDate.parse(task.getMarker()) : LocalDate.now().minusMonths(1);

		OAuthRequest profileRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		service.signRequest(task.getToken(), profileRequest);
		Response profileResponse = profileRequest.send();
		Preconditions.checkState(profileResponse.isSuccessful(), "Failed to get profile for task <%s>", task.getId());
		FitbitProfileResult profile = new FitbitProfileResult(parseObject(profileResponse));

		for (LocalDate date = fromDate.plusDays(1); !date.isAfter(lastDate); date = date.plusDays(1)) {

			// OAuthRequest sleepRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/sleep/date/" + date + ".json");
			// service.signRequest(task.getToken(), sleepRequest);
			// Response sleepResponse = sleepRequest.send();
			// Preconditions.checkState(sleepResponse.isSuccessful(), "Failed to get sleep for task <%s>", task.getId());
			// events.addAll(new FitbitSleepResult(parseObject(sleepResponse), task.getPrincipal(), profile.getTimezone()).getEvents());

			OAuthRequest activitiesRequest = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/activities/date/" + date + ".json");
			activitiesRequest.addHeader("Accept-Language", profile.getDistanceLocale());
			service.signRequest(task.getToken(), activitiesRequest);
			Response activitiesResponse = activitiesRequest.send();
			Preconditions.checkState(activitiesResponse.isSuccessful(), "Failed to get activities for task <%s>", task.getId());
			events.addAll(new FitbitActivitiesResult(parseObject(activitiesResponse), task.getTag(), task.getPrincipal(), date.toDateTimeAtStartOfDay(profile.getTimezone()), profile.getDistanceUnit(), profile.getHeightUnit()).getEvents());
		}

		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran fitbit task", "reverted fitbit task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), new DateTime(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), lastDate.toString())
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		for (Event event : events) {
			// System.out.println("[event] " + event);
			command.add(new CreateEventCommand(task.getPrincipal(), task.getBucketId(), event));
		}

		return command;
	}
}
