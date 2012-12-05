package com.zenobase.tasks;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;
import com.google.common.base.Preconditions;

import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;

public abstract class FitbitTaskManagerSupport extends OAuthTaskManager {

	public FitbitTaskManagerSupport(Class<? extends Api> apiClass,
			String apiKey, String apiSecret, String callbackUrl) {
		super(apiClass, apiKey, apiSecret, callbackUrl);
	}

	protected LocalDate getLastDate(OAuthTask task, OAuthService service) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		service.signRequest(task.getToken(), request);
		Response response = request.send();
		Preconditions.checkState(response.isSuccessful(), "Failed to get devices for task <%s>", task.getId());
		return new FitbitDevicesResult(parseArray(response)).getLastDate();
	}

	protected LocalDate getFromDate(Task task) {
		return task.getMarker() != null ? LocalDate.parse(task.getMarker()) : LocalDate.now().minusMonths(1);
	}

	protected FitbitProfileResult getProfile(OAuthTask task, OAuthService service) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		service.signRequest(task.getToken(), request);
		Response response = request.send();
		Preconditions.checkState(response.isSuccessful(), "Failed to get profile for task <%s>", task.getId());
		return new FitbitProfileResult(parseObject(response));
	}

	protected CompoundCommand createCommand(Task task, List<Event> events, LocalDate lastDate) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran fitbit-highres task", "reverted fitbit-highres task");
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