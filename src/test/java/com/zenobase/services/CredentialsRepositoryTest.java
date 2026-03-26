package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.concurrent.TimeUnit;

import com.google.common.collect.Lists;
import com.google.common.util.concurrent.Uninterruptibles;
import org.joda.time.DateTime;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Callback;
import com.zenobase.models.Identity;
import com.zenobase.tasks.Credentials;

public class CredentialsRepositoryTest extends OpenSearchTestSupport {

	private static final Identity ME = new Identity("me");
	private static final Identity YOU = new Identity("you");
	private static final String TYPE = "test";

	private CredentialsRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new CredentialsRepository(getManager());
	}

	@Test
	public void test() {
		Credentials credentials = new Credentials(TYPE, new Identity());
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
	public void testFindWithPaging() {
		List<Credentials> credentials = insert(11);
		assertThat(repository.find(0, 10)).hasTotal(credentials.size()).isEqualTo(credentials.subList(0, 10));
		assertThat(repository.find(10, 10)).hasTotal(credentials.size()).isEqualTo(credentials.subList(10, 11));
		assertThat(repository.find(20, 10)).hasTotal(credentials.size()).isEmpty();
	}

	@Test
	public void testFindWithCallback() {
		List<Credentials> expected = insert(11);
		Callback<Credentials> callback = mock(Callback.class);
		repository.find(new CredentialsQuery(), callback);
		verifyInteractions(callback, expected);
	}

	@Test
	public void testFindTypeAndPrincipalEqualTo() {
		Credentials c1 = insert("foo", ME);
		insert("bar", ME);
		insert("foo", YOU);
		assertThat(repository.find(ME, "foo")).isEqualTo(c1);
	}

	@Test
	public void testFindCreatedBefore() {
		Credentials c1 = new Credentials(TYPE, ME, DateTime.now());
		Credentials c2 = new Credentials(TYPE, ME, DateTime.now().minusHours(2));
		store(c1);
		store(c2);
		Callback<Credentials> callback = mock(Callback.class);
		repository.find(new CredentialsQuery().createdBefore(DateTime.now().minusHours(1)), callback);
		verifyInteractions(callback, List.of(c2));
	}

	@Test
	public void testFindNotAuthorized() {
		Credentials c1 = new Credentials(TYPE, ME);
		Credentials c2 = new Credentials(TYPE, ME);
		c2.setAuthorizationUrl("urn:test");
		store(c1);
		store(c2);
		Callback<Credentials> callback = mock(Callback.class);
		repository.find(new CredentialsQuery().notAuthorized(), callback);
		verifyInteractions(callback, List.of(c2));
	}

	private List<Credentials> insert(int size) {
		List<Credentials> credentialsList = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			Credentials credentials = new Credentials(TYPE, ME);
			credentialsList.add(credentials);
			Uninterruptibles.sleepUninterruptibly(
					5, TimeUnit.MILLISECONDS); // sleep so we can sort by creation time later
			store(credentials);
		}
		return Lists.reverse(credentialsList);
	}

	private Credentials insert(String type, Identity principal) {
		Credentials credentials = new Credentials(type, principal);
		store(credentials);
		return credentials;
	}

	private void store(Credentials credentials) {
		repository.store(credentials, DateTime.now());
	}
}
