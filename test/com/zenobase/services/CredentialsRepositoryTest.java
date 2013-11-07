package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.joda.time.DateTime;
import org.junit.Before;
import org.junit.Test;
import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;

import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;

public class CredentialsRepositoryTest extends ElasticSearchTestSupport {

	private final String type = "test";
	private CredentialsRepository repository;

	@Before
	public void setUp() {
		repository = new CredentialsRepository(getManager());
	}

	@Test
	public void testCRUD() {
		Credentials credentials = new Credentials(type, new Identity());
		assertThat(repository.find(credentials.getId())).isNull();
		assertThat(repository.delete(credentials.getId())).isFalse();
		repository.store(credentials, DateTime.now());
		assertThat(repository.find(credentials.getId()).toJson()).isEqualTo(credentials.toJson());
		credentials.setAuthorizationUrl("http://localhost/");
		repository.update(credentials, DateTime.now());
		assertThat(repository.find(credentials.getId()).toJson()).isEqualTo(credentials.toJson());
		assertThat(repository.delete(credentials.getId())).isTrue();
		assertThat(repository.find(credentials.getId())).isNull();
	}

	@Test
	public void testFindAll() {
		List<Credentials> credentials = fill(20, new Identity());
		assertThat(repository.find(0, 10)).hasTotal(credentials.size()).isEqualTo(credentials.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(credentials.size()).isEqualTo(credentials.subList(10, 20));
		assertThat(repository.find(20, 10)).hasTotal(credentials.size()).isEqualTo(Collections.emptyList());
	}

	@Test
	public void testFindByPrincipal() {
		Credentials expected = new Credentials(type, new Identity());
		store(new Credentials(type, new Identity()));
		assertThat(repository.find(Credentials.PRINCIPAL.getName(), expected.getPrincipal().toString(), 0, 10)).hasTotal(0);
		store(expected);
		assertThat(repository.find(expected.getPrincipal(), type)).isEqualTo(expected);
	}

	@Test
	public void testFindByPrincipalAndType() {
		Credentials expected = new Credentials(type, new Identity());
		store(new Credentials("foo", expected.getPrincipal()));
		store(new Credentials(expected.getType(), new Identity()));
		assertThat(repository.find(expected.getPrincipal(), expected.getType())).isNull();
		store(expected);
		assertThat(repository.find(expected.getPrincipal(), expected.getType())).isEqualTo(expected);
	}

	private List<Credentials> fill(int size, Identity principal) {
		List<Credentials> credentialsList = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Credentials credentials = new Credentials(type, principal);
			credentialsList.add(credentials);
			Uninterruptibles.sleepUninterruptibly(5, TimeUnit.MILLISECONDS); // credentials will be returned in order of creation time
			store(credentials);
		}
		return Lists.reverse(credentialsList);
	}

	private void store(Credentials credentials) {
		repository.store(credentials, DateTime.now());
	}
}
