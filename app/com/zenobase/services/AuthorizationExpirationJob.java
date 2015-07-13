package com.zenobase.services;

import javax.inject.Inject;

import com.zenobase.commands.DeleteAuthorizationCommand;
import com.zenobase.common.Callback;
import com.zenobase.oauth.Authorization;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import play.Logger;

public class AuthorizationExpirationJob extends Job {

	private static final Period MAX_AGE = Period.minutes(5);

	private final AuthorizationRepository authorizations;
	private final CommandDispatcher dispatcher;

	@Inject
	public AuthorizationExpirationJob(AuthorizationRepository authorizations, CommandDispatcher dispatcher) {
		super("expire authorizations", new LocalTime(3, 0), Period.days(1));
		this.authorizations = authorizations;
		this.dispatcher = dispatcher;
	}

	@Override
	public void run() {
		Logger.info("Expiring authorizations...");
		AuthorizationQuery query = new AuthorizationQuery().createdBefore(DateTime.now().minus(MAX_AGE)).clientIsNull();
		authorizations.find(query, new Callback<Authorization>() {
			@Override
			public void call(Authorization authorization) {
				dispatcher.dispatch(new DeleteAuthorizationCommand(authorization.getPrincipal(), authorization));
			}
		});
	}
}
