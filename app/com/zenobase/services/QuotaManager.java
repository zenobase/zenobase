package com.zenobase.services;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;
import play.Logger.ALogger;

import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class QuotaManager {

	public static final int DEFAULT_QUOTA = 50000;

	private final ALogger log = Logger.of("quota");
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
			log.warn("{} has {} but needs {}", principal, quota.getRemaining(), cost);
			throw new QuotaException(quota.getRemaining(), cost);
		}
	}

	public Quota getQuota(Identity principal) {
		return new Quota(getQuotaLimit(principal), getQuotaUsed(principal));
	}

	private int getQuotaLimit(Identity principal) {
		User user = users.find(principal);
		return user != null && user.getQuota() != null ? user.getQuota() : DEFAULT_QUOTA;
	}

	private int getQuotaUsed(Identity principal) {
		return commands.getTotalCost(principal, timeAtStartOfMonth());
	}

	private static DateTime timeAtStartOfMonth() {
		return DateTime.now(DateTimeZone.UTC).withDayOfMonth(1).withTimeAtStartOfDay();
	}
}
