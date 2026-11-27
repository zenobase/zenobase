package com.zenobase.tasks.withings;

import com.google.common.base.MoreObjects;
import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import com.zenobase.commands.Command;
import com.zenobase.commands.CompoundCommand;
import com.zenobase.commands.CreateEventsCommand;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.commands.UpdateTaskCommand;
import com.zenobase.tasks.Credentials;
import com.zenobase.tasks.InvalidStatusException;
import com.zenobase.tasks.InvalidTokenException;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.RateLimiters;
import com.zenobase.tasks.Task;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

abstract class WithingsTaskManagerSupport<T extends Task> extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiters.create(2);

	private final Class<T> taskClass;

	WithingsTaskManagerSupport(String type, Class<T> taskClass, WithingsCredentialsManager credentialsManager) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		Token token = Objects.requireNonNull(credentials.getToken());
		if (credentials.isExpired() || !Strings.isNullOrEmpty(token.getSecret())) {
			reauthorize(credentials); // oauth1 token
		}
		return safeExecute(task.as(taskClass), credentials, token);
	}

	abstract Command safeExecute(T task, OAuthCredentials credentials, Token token);

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}

	protected void checkStatus(WithingsResult result, OAuthRequest request, OAuthCredentials credentials) {
		if (WithingsCredentialsManager.RESPONSE_CODES_UNAUTHORIZED.contains(result.getStatus())) {
			throw new InvalidTokenException(credentials);
		}
		if (result.getStatus() != 0) {
			throw new InvalidStatusException(request.getCompleteUrl(), result.getStatus(), result.node.toString());
		}
	}

	/** The marker after a successful run, given the current task and the fetched result. */
	@Nullable
	String nextMarker(T task, WithingsResult result) {
		return MoreObjects.firstNonNull(result.getMarker(), task.getMarker());
	}

	protected Command createCommand(T task, OAuthCredentials credentials, Token expiredToken, WithingsResult result) {
		var command = new CompoundCommand(
			task.getPrincipal(),
			"ran " + getType() + " task",
			"reverted " + getType() + " task"
		);
		command.add(
			UpdateTaskCommand.builder(task)
				.set(Task.COMPLETED, task.getCompleted(), DateTime.now(DateTimeZone.UTC))
				.set(Task.STATUS, task.getStatus(), Task.Status.SUCCESS)
				.set(Task.MARKER, task.getMarker(), nextMarker(task, result))
				.set(Task.UNDO, task.getUndoId(), command.getId())
				.build()
		);
		if (!Objects.equals(credentials.getToken(), expiredToken)) {
			command.add(
				UpdateCredentialsCommand.builder(credentials)
					.with(Credentials.CREDENTIALS)
					.set(OAuthCredentials.TOKEN, expiredToken, credentials.getToken())
					.build()
			);
		}
		if (!result.getEvents().isEmpty()) {
			command.add(new CreateEventsCommand(task.getPrincipal(), task.getBucketId(), result.getEvents()));
		}
		return command;
	}
}
