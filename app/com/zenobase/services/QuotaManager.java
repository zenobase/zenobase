package com.zenobase.services;

import javax.inject.Inject;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import play.Logger;

import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class QuotaManager {

	private static final int DEFAULT_QUOTA = 1000;

	private final UserRepository users;
	private final CommandRepository commands;

	@Inject
	public QuotaManager(UserRepository users, CommandRepository commands) {
		this.users = users;
		this.commands = commands;
	}

	public void spend(Identity principal, int cost) {
		int remaining = getQuotaRemaining(principal);
		Logger.debug("Quota remaining: " + remaining +  ", required: " + cost);
		if (remaining < cost) {
			throw new QuotaException(remaining, cost);
		}
	}

	private int getQuotaRemaining(Identity principal) {
		return getQuotaLimit(principal) - getQuotaUsed(principal);
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
