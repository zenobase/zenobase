package com.zenobase.tasks.fitbit;

import java.util.List;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Verb;
import play.Logger;
import com.google.common.util.concurrent.RateLimiter;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public abstract class FitbitTaskManagerSupport<T extends Task> extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(10); // actually 400 per hour per user

	private final Class<T> taskClass;

	protected FitbitTaskManagerSupport(String type, Class<T> taskClass, FitbitCredentialsManager credentialsManager) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public final Command execute(Task task, OAuthCredentials credentials) {
		try {
			return safeExecute(task.as(taskClass), credentials);
		} catch (InvalidStatusException e) {
			if (e.getStatus() == 429) { // reached rate limit
				Logger.warn("Hit rate limit and couldn't run task: {}", task.getId());
				return null;
			}
			throw e;
		}
	}

	protected abstract Command safeExecute(T task, OAuthCredentials credentials);

	protected void checkRateLimit() {
		RATE_LIMITER.acquire();
	}

	protected LocalDate getLastDate(DeviceType deviceType, Task task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		Response response = send(request, credentials);
		return new FitbitDevicesResult(parseArray(response)).getLastDate(deviceType);
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		checkRateLimit();
		return super.send(request, credentials);
	}

	protected LocalDate getFromDate(Task task) {
		return LocalDate.parse(task.getMarker());
	}

	protected LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	protected FitbitProfileResult getProfile(Task task, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		Response response = send(request, credentials);
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
