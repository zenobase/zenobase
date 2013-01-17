package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

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
	public void testFindClientOnly() {
		Identity me = new Identity();
		List<Authorization> authorizations = fill(20, me);
		assertThat(repository.find(Authorization.PRINCIPAL.getName(), me.toString(), true, 0, 20)).hasTotal(authorizations.size() / 2);
	}

	@Test
	public void testFindByPrincipal() {
		Identity me = new Identity();
		Identity you = new Identity();
		List<Authorization> mine = fill(2, me);
		List<Authorization> yours = fill(3, you);
		assertThat(repository.find(Authorization.PRINCIPAL.getName(), me.toString(), false, 0, 10)).hasTotal(mine.size()).isEqualTo(mine);
		assertThat(repository.find(Authorization.PRINCIPAL.getName(), you.toString(), false, 0, 10)).hasTotal(yours.size()).isEqualTo(yours);
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
}
