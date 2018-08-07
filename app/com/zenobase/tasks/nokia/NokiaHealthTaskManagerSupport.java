package com.zenobase.tasks.nokia;

import com.google.common.base.Strings;
import com.google.common.util.concurrent.RateLimiter;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;

import com.zenobase.commands.Command;
import com.zenobase.tasks.OAuthCredentials;
import com.zenobase.tasks.OAuthTaskManager;
import com.zenobase.tasks.Task;

abstract class NokiaHealthTaskManagerSupport<T extends Task> extends OAuthTaskManager {

	private static final RateLimiter RATE_LIMITER = RateLimiter.create(2);

	private final Class<T> taskClass;

	NokiaHealthTaskManagerSupport(String type, Class<T> taskClass, NokiaHealthCredentialsManager credentialsManager) {
		super(type, credentialsManager);
		this.taskClass = taskClass;
	}

	@Override
	public Command execute(Task task, OAuthCredentials credentials) {
		if (!Strings.isNullOrEmpty(credentials.getToken().getSecret())) {
			reauthorize(credentials); // oauth1 token
		}
		return safeExecute(task.as(taskClass), credentials);
	}

	abstract Command safeExecute(T task, OAuthCredentials credentials);

	@Override
	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		RATE_LIMITER.acquire();
		return super.send(request, credentials);
	}
}
