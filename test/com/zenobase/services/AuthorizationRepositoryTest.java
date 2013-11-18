package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.common.Callback;
import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationRepositoryTest extends ElasticSearchTestSupport {

	private static final Identity ME = new Identity();
	private static final Identity YOU = new Identity();

	private AuthorizationRepository repository;

	@Before
	public void setUp() {
		repository = new AuthorizationRepository(getManager());
	}

	@Test
	public void test() {
		Authorization authorization = new Authorization(new Identity(), new Identity(), Generator.id());
		assertThat(repository.find(authorization.getId())).isNull();
		assertThat(repository.delete(authorization.getId())).isFalse();
		repository.store(authorization, DateTime.now());
		assertThat(repository.find(authorization.getId()).toJson()).isEqualTo(authorization.toJson());
		assertThat(repository.delete(authorization.getId())).isTrue();
		assertThat(repository.find(authorization.getId())).isNull();
	}

	@Test
	public void testFindWithPaging() {
		List<Authorization> expected = insert(11);
		assertThat(repository.find(0, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(expected.size()).isEqualTo(expected.subList(10, 11));
		assertThat(repository.find(20, 10)).hasTotal(expected.size()).isEmpty();
	}

	@Test
	public void testFindWithCallback() {
		List<Authorization> expected = insert(11);
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery(), callback);
		verifyInteractions(callback, expected);
	}

	@Test
	public void testFindPrincipalEqualTo() {
		Authorization a1 = insert(ME, null, null);
		insert(YOU, null, null);
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery().principalEqualTo(ME), callback);
		verifyInteractions(callback, ImmutableList.of(a1));
	}

	@Test
	public void testFindClientEqualTo() {
		Authorization a1 = insert(ME, ME, null);
		Authorization a2 = insert(YOU, ME, null);
		insert(ME, null, null);
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery().clientEqualTo(ME), callback);
		verifyInteractions(callback, ImmutableList.of(a1, a2));
	}

	@Test
	public void testFindClientIsNull() {
		Authorization a1 = insert(ME, null, null);
		Authorization a2 = insert(YOU, null, null);
		insert(ME, YOU, null);
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery().clientIsNull(), callback);
		verifyInteractions(callback, ImmutableList.of(a1, a2));
	}

	@Test
	public void testFindClientNotNull() {
		Authorization a1 = insert(ME, ME, null);
		Authorization a2 = insert(ME, YOU, null);
		insert(ME, null, null);
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery().clientNotNull(), callback);
		verifyInteractions(callback, ImmutableList.of(a1, a2));
	}

	@Test
	public void testScopeEqualTo() {
		String scope = "foo";
		Authorization a1 = insert(ME, null, scope);
		insert(ME, null, "bar");
		insert(ME, null, null);
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery().scopeEqualTo(scope), callback);
		verifyInteractions(callback, ImmutableList.of(a1));
	}

	@Test
	public void testCreatedBefore() {
		DateTime t = DateTime.now().minusMonths(1);
		Authorization a1 = insert(ME, DateTime.now().minusMonths(2));
		insert(ME, DateTime.now());
		Callback<Authorization> callback = mock(Callback.class);
		repository.find(new AuthorizationQuery().createdBefore(t), callback);
		verifyInteractions(callback, ImmutableList.of(a1));
	}

	private List<Authorization> insert(int size) {
		List<Authorization> authorizations = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			authorizations.add(insert(ME, null, null));
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // tasks will be returned in order of creation time
		}
		return Lists.reverse(authorizations);
	}

	private Authorization insert(Identity principal, Identity client, String scope) {
		return insert(new Authorization(principal, client, scope));
	}

	private Authorization insert(Identity principal, DateTime created) {
		return insert(new Authorization(principal, null, null, created));
	}

	private Authorization insert(Authorization authorization) {
		repository.store(authorization, DateTime.now());
		return authorization;
	}
}
