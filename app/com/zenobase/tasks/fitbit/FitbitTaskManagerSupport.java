package com.zenobase.tasks.fitbit;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.builder.api.Api;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import org.scribe.oauth.OAuthService;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.OAuthTask;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public abstract class FitbitTaskManagerSupport extends OAuthTaskManager {

	public FitbitTaskManagerSupport(Api provider, String apiKey, String apiSecret, String callbackUrl) {
		super(provider, apiKey, apiSecret, callbackUrl);
	}

	protected LocalDate getLastDate(OAuthTask task, OAuthService service) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		service.signRequest(task.getToken(), request);
		Response response = request.send();
		checkResponse(task, request, response);
		return new FitbitDevicesResult(parseArray(response)).getLastDate();
	}

	protected LocalDate getFromDate(Task task) {
		return LocalDate.parse(task.getMarker());
	}

	protected LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	protected FitbitProfileResult getProfile(OAuthTask task, OAuthService service) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		service.signRequest(task.getToken(), request);
		Response response = request.send();
		checkResponse(task, request, response);
		return new FitbitProfileResult(parseObject(response));
	}

	protected Command createCommand(Task task, List<Event> events, LocalDate lastDate) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
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
