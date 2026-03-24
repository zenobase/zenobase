package com.zenobase.tasks.fitbit;

import java.util.List;

import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.LocalDate;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import org.scribe.model.Verb;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.models.Event;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

public abstract class FitbitTaskManagerSupport<T extends Task> extends OAuthTaskManager {

	private static final Logger logger = LoggerFactory.getLogger(FitbitTaskManagerSupport.class);

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(10); // actually 400 per hour per user

	private final Class<T> taskClass;

	protected FitbitTaskManagerSupport(String type, Class<T> taskClass, FitbitCredentialsManager credentialsManager) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public final Command execute(Task task, OAuthCredentials credentials) {
		Token token = credentials.getToken();
		if (credentials.isExpired() || !Strings.isNullOrEmpty(credentials.getToken().getSecret())) {
			reauthorize(credentials);
		}
		try {
			return safeExecute(task.as(taskClass), credentials, token);
		} catch (InvalidStatusException e) {
			if (e.getStatus() == 429) { // reached rate limit
				logger.warn("Hit rate limit and couldn't run task: {}", task.getId());
				return null;
			}
			throw e;
		}
	}

	protected abstract Command safeExecute(T task, OAuthCredentials credentials, Token token);

	protected LocalDate getLastDate(DeviceType deviceType, OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/devices.json");
		Response response = send(request, credentials);
		return new FitbitDevicesResult(parseArray(response)).getLastDate(deviceType);
	}

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	protected LocalDate getFromDate(Task task) {
		return LocalDate.parse(task.getMarker());
	}

	protected LocalDate parseMarker(String marker) {
		return marker != null ? DateTime.parse(marker).toLocalDate() : LocalDate.now().withDayOfMonth(1);
	}

	protected FitbitProfileResult getProfile(OAuthCredentials credentials) {
		OAuthRequest request = new OAuthRequest(Verb.GET, "https://api.fitbit.com/1/user/-/profile.json");
		Response response = send(request, credentials);
		return new FitbitProfileResult(parseObject(response));
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, LocalDate lastDate, Token expiredToken) {
		return createCommand(task, credentials, events, lastDate.toString(), expiredToken);
	}

	protected Command createCommand(Task task, OAuthCredentials credentials, List<Event> events, String marker, Token expiredToken) {
		CompoundCommand command = new CompoundCommand(task.getPrincipal(), "ran " + getType() + " task", "reverted " + getType() + " task");
		command.add(UpdateTaskCommand.builder(task)
			.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
			.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
			.set(Task.MARKER, task.getMarker(), marker)
			.set(Task.UNDO, task.getUndoId(), command.getId())
			.build());
		if (!Objects.equal(credentials.getToken(), expiredToken)) {
			command.add(UpdateCredentialsCommand.builder(credentials)
				.with(Credentials.CREDENTIALS)
				.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
				.build());
		}
		if (!events.isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), events));
		}
		return command;
	}
}
