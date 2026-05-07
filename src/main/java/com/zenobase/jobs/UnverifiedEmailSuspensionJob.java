package com.zenobase.jobs;

import com.zenobase.commands.SuspendUserCommand;
import com.zenobase.queries.UserQuery;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.LocalTime;
import org.joda.time.Period;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class UnverifiedEmailSuspensionJob extends Job {

	private static final Logger logger = LoggerFactory.getLogger(UnverifiedEmailSuspensionJob.class);

	private static final Period MAX_AGE = Period.days(30);

	private final UserRepository users;
	private final CommandDispatcher dispatcher;

	@Inject
	public UnverifiedEmailSuspensionJob(UserRepository users, CommandDispatcher dispatcher) {
		super("suspend unverified users", new LocalTime(4, 30), Period.days(1));
		this.users = users;
		this.dispatcher = dispatcher;
	}

	@Override
	public void run() {
		logger.info("Suspending unverified users older than {}...", MAX_AGE);
		var query = new UserQuery().isVerified(false).isSuspended(false).createdBefore(DateTime.now().minus(MAX_AGE));
		users.find(query, user -> {
			String name = user.getName();
			if (name == null) {
				return;
			}
			logger.info("Suspending unverified user {}", name);
			dispatcher.dispatch(new SuspendUserCommand(user.asIdentity(), name, true));
		});
	}
}
