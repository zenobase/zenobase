package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.junit.Test;

import com.zenobase.models.User;

public class QuotaManagerTest {

	private final UserRepository users = mock(UserRepository.class);
	private final CommandRepository commands = mock(CommandRepository.class);
	private final User user = new User("jdoe");
	private final QuotaManager quotas = new QuotaManager(users, commands);

	@Test
	public void testGuestQuota() {
		Quota quota = quotas.getQuota(user.asIdentity());
		assertThat(quota.getLimit()).isEqualTo(QuotaManager.DEFAULT_QUOTA);
		assertThat(quota.getRemaining()).isEqualTo(QuotaManager.DEFAULT_QUOTA);
	}

	@Test
	public void testDefaultQuota() {
		when(users.find(user.getId())).thenReturn(user);
		Quota quota = quotas.getQuota(user.asIdentity());
		assertThat(quota.getLimit()).isEqualTo(QuotaManager.DEFAULT_QUOTA);
		assertThat(quota.getRemaining()).isEqualTo(QuotaManager.DEFAULT_QUOTA);
	}

	@Test
	public void testCustomQuota() {
		final int limit = 5000;
		user.setQuota(limit);
		when(users.find(user.asIdentity())).thenReturn(user);
		Quota quota = quotas.getQuota(user.asIdentity());
		assertThat(quota.getLimit()).isEqualTo(limit);
		assertThat(quota.getRemaining()).isEqualTo(limit);
	}

	@Test
	public void testPartiallyUsedQuota() {
		final int spent = 1000;
		when(users.find(user.getId())).thenReturn(user);
		when(commands.getTotalCost(eq(user.asIdentity()), any(DateTime.class))).thenReturn(1000);
		Quota quota = quotas.getQuota(user.asIdentity());
		assertThat(quota.getLimit()).isEqualTo(QuotaManager.DEFAULT_QUOTA);
		assertThat(quota.getRemaining()).isEqualTo(QuotaManager.DEFAULT_QUOTA - spent);
	}

	@Test
	public void testSpend() {
		quotas.spend(user.asIdentity(), 1000);
	}

	@Test(expected = QuotaException.class)
	public void testOverspend() {
		quotas.spend(user.asIdentity(), 10000);
	}
}
