package com.zenobase.services;

import javax.inject.Inject;

import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.common.Callback;
import com.zenobase.tasks.Credentials;

import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import play.Logger;

public class CredentialsCleanupJob extends Job {

	private static final Period MAX_AGE = Period.minutes(30);

	private final CredentialsRepository credentials;
	private final CommandDispatcher dispatcher;

	@Inject
	public CredentialsCleanupJob(CredentialsRepository credentials, CommandDispatcher dispatcher) {
		super("clean up credentials", new LocalTime(4, 0), Period.days(1));
		this.credentials = credentials;
		this.dispatcher = dispatcher;
	}

	@Override
	public void run() {
		Logger.info("Cleaning up credentials...");
		CredentialsQuery query = new CredentialsQuery().notAuthorized().createdBefore(DateTime.now().minus(MAX_AGE));
		credentials.find(query, new Callback<Credentials>() {
			@Override
			public void call(Credentials credentials) {
				dispatcher.dispatch(new DeleteCredentialsCommand(credentials.getPrincipal(), credentials));
			}
		});
	}
}
