package com.zenobase.services;

import static org.assertj.core.api.Assertions.assertThat;

import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.repositories.UserRepository;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

public class UserLookupTest {

	private final UserRepository repository = Mockito.mock(UserRepository.class);
	private final UserLookup lookup = new UserLookup(repository);

	@Test
	public void testGetIdentityFromId() {
		Identity identity = new Identity();
		assertThat(lookup.getIdentity(identity.id())).isEqualTo(identity);
	}

	@Test
	public void testGetIdentityFromName() {
		User user = new User("jdoe");
		Mockito.when(repository.find(user.getName())).thenReturn(user);
		assertThat(lookup.getIdentity('@' + user.getName())).isEqualTo(user.asIdentity());
	}

	@Test
	public void testGetIdentityFromNameNotFound() {
		User user = new User("jdoe");
		assertThat(lookup.getIdentity('@' + user.getName())).isNull();
	}

	@Test
	public void testGetUserFromId() {
		User user = new User("jdoe");
		Mockito.when(repository.find(user.asIdentity())).thenReturn(user);
		assertThat(lookup.getUser(user.getId())).isEqualTo(user);
	}

	@Test
	public void testGetUserFromIdNotFound() {
		Identity identity = new Identity();
		User found = lookup.getUser(identity.id());
		assertThat(found.getId()).isEqualTo(identity.id());
		assertThat(found.getName()).isNull();
	}

	@Test
	public void testGetUserFromName() {
		User user = new User("jdoe");
		Mockito.when(repository.find(user.getName())).thenReturn(user);
		assertThat(lookup.getUser('@' + user.getName())).isEqualTo(user);
	}

	@Test
	public void testGetUserFromNameNotFound() {
		User user = new User("jdoe");
		assertThat(lookup.getUser('@' + user.getName())).isNull();
	}
}
