package com.zenobase.services;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.CommandRepository;
import com.zenobase.repositories.UserRepository;
import jakarta.inject.Inject;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QuotaManager {

	public static final int DEFAULT_QUOTA = 50000;

	private static final Logger logger = LoggerFactory.getLogger(QuotaManager.class);

	private final UserRepository users;
	private final CommandRepository commands;

	@Inject
	public QuotaManager(UserRepository users, CommandRepository commands) {
		this.users = users;
		this.commands = commands;
	}

	public void spend(Identity principal, int cost) {
		Quota quota = getQuota(principal);
		if (quota.getRemaining() < cost) {
			logger.warn("{} has {} but needs {}", principal, quota.getRemaining(), cost);
			throw new QuotaException(quota.getRemaining(), cost);
		}
	}

	public Quota getQuota(Identity principal) {
		return new Quota(getQuotaLimit(principal), getQuotaUsed(principal));
	}

	private int getQuotaLimit(Identity principal) {
		User user = users.find(principal);
		if (user == null) {
			throw new IllegalStateException("No user for principal " + principal);
		}
		return user.getQuota() != null ? user.getQuota() : DEFAULT_QUOTA;
	}

	private int getQuotaUsed(Identity principal) {
		return commands.getTotalCost(principal, timeAtStartOfMonth());
	}

	private static DateTime timeAtStartOfMonth() {
		return DateTime.now(DateTimeZone.UTC).withDayOfMonth(1).withTimeAtStartOfDay();
	}
}
