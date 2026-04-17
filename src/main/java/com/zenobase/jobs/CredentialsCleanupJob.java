package com.zenobase.jobs;

import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.commands.DeleteCredentialsCommand;
import com.zenobase.queries.CredentialsQuery;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.services.CommandDispatcher;

public class CredentialsCleanupJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(CredentialsCleanupJob.class);

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
		logger.info("Cleaning up credentials...");
		var query = new CredentialsQuery()
				.notAuthorized()
				.createdBefore(DateTime.now().minus(MAX_AGE));
		credentials.find(
				query,
				credentials ->
						dispatcher.dispatch(new DeleteCredentialsCommand(credentials.getPrincipal(), credentials)));
	}
}
