package com.zenobase.services;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.DeleteAuthorizationCommand;

public class AuthorizationExpirationJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(AuthorizationExpirationJob.class);

	private static final Period MAX_AGE = Period.months(1);

	private final AuthorizationRepository authorizations;
	private final CommandDispatcher dispatcher;

	public AuthorizationExpirationJob(AuthorizationRepository authorizations, CommandDispatcher dispatcher) {
		super("expire authorizations", new LocalTime(3, 0), Period.days(1));
		this.authorizations = authorizations;
		this.dispatcher = dispatcher;
	}

	@Override
	public void run() {
		logger.info("Expiring authorizations...");
		var query = new AuthorizationQuery()
				.createdBefore(DateTime.now().minus(MAX_AGE))
				.clientIsNull();
		authorizations.find(
				query,
				authorization -> dispatcher.dispatch(
						new DeleteAuthorizationCommand(authorization.getPrincipal(), authorization)));
	}
}
