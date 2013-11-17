package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.joda.time.Period;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationRepositoryTest extends ElasticSearchTestSupport {

	private AuthorizationRepository repository;

	@Before
	public void setUp() {
		repository = new AuthorizationRepository(getManager());
	}

	@Test
	public void testCreateReadDelete() {

		Authorization authorization = new Authorization(new Identity(), new Identity(), Generator.id());
		assertThat(repository.find(authorization.getId())).isNull();
		assertThat(repository.delete(authorization.getId())).isFalse();

		repository.store(authorization, DateTime.now());
		assertThat(repository.find(authorization.getId()).toJson()).isEqualTo(authorization.toJson());

		assertThat(repository.delete(authorization.getId())).isTrue();
		assertThat(repository.find(authorization.getId())).isNull();
	}

	@Test
	public void testFindAll() {
		List<Authorization> authorizations = fill(20, new Identity());
		assertThat(repository.find(0, 10)).hasTotal(authorizations.size()).isEqualTo(authorizations.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(authorizations.size()).isEqualTo(authorizations.subList(10, 20));
	}

	@Test
	public void testFindByUserWithClient() {
		Identity me = new Identity();
		List<Authorization> authorizations = fill(20, me);
		assertThat(repository.find(me, Boolean.TRUE, 0, 20)).hasTotal(authorizations.size() / 2);
	}

	@Test
	public void testFindByUserWithoutClient() {
		Identity me = new Identity();
		List<Authorization> authorizations = fill(20, me);
		assertThat(repository.find(me, Boolean.FALSE, 0, 20)).hasTotal(authorizations.size() / 2);
	}

	@Test
	public void testFindByUser() {
		Identity me = new Identity();
		Identity you = new Identity();
		List<Authorization> mine = fill(2, me);
		List<Authorization> yours = fill(3, you);
		assertThat(repository.find(me, null, 0, 10)).hasTotal(mine.size()).isEqualTo(mine);
		assertThat(repository.find(you, null, 0, 10)).hasTotal(yours.size()).isEqualTo(yours);
	}

	private List<Authorization> fill(int size, Identity principal) {
		List<Authorization> authorizations = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Authorization authorization = new Authorization(principal, i % 2 == 0 ? new Identity() : null, Generator.id());
			authorizations.add(authorization);
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // tasks will be returned in order of creation time
			repository.store(authorization, DateTime.now());
		}
		return Lists.reverse(authorizations);
	}

	@Test
	public void testFindExisting() {
		Identity me = new Identity();
		Identity you = new Identity();
		String s1 = "foo";
		String s2 = "bar";
		Authorization a1 = add(me, you, s1);
		Authorization a2 = add(me, you, null);
		Authorization a3 = add(me, null, null);
		Authorization a4 = add(you, me, s1);
		Authorization a5 = add(you, me, s2);
		assertThat(repository.find(me, you, s1)).as("me to you with scope").isEqualTo(a1);
		assertThat(repository.find(me, you, null)).as("me to you without scope").isEqualTo(a2);
		assertThat(repository.find(me, null, null)).as("me without scope").isEqualTo(a3);
		assertThat(repository.find(you, me, s1)).as("you to me with scope").isEqualTo(a4);
		assertThat(repository.find(you, me, s2)).as("you to me with second scope").isEqualTo(a5);
	}

	private Authorization add(Identity principal, Identity client, String scope) {
		Authorization authorization = new Authorization(principal, client, scope);
		repository.store(authorization, DateTime.now());
		return authorization;
	}

	@Test
	public void testFindExpired() {
		add(false, DateTime.now());
		add(true, DateTime.now().minusMonths(2));
		Authorization expected = add(false, DateTime.now().minusMonths(2));
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(Period.months(1), callback);
		verify(callback).call(expected);
		verifyNoMoreInteractions(callback);
	}

	private Authorization add(boolean withClient, DateTime created) {
		Authorization authorization = new Authorization(new Identity(), withClient ? new Identity() : null, null, created);
		repository.store(authorization, DateTime.now());
		return authorization;
	}
}
