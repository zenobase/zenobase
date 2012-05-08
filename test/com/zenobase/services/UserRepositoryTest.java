package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

import java.util.List;

import org.junit.Before;
import org.junit.Test;
import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

import com.zenobase.common.Callback;
import com.zenobase.models.User;

public class UserRepositoryTest extends ElasticSearchTestSupport {

	private UserRepository repository;

	@Before
	public void setUp() {
		repository = new UserRepository(getManager());
	}

	@Test
	public void testCrudUser() {

		// create user
		User user = new User("tester");
		user.setEmail("jdoe@zenobase.com");
		user.setPassword("secret123");

		assertThat(repository.isEmpty()).isTrue();
		assertThat(repository.exists(user.getName())).isFalse();
		assertThat(repository.find(user.asIdentity())).isNull();

		// store and retrieve user
		repository.store(user);
		assertThat(repository.isEmpty()).isFalse();
		assertThat(repository.find(user.getName()).toJson()).isEqualTo(user.toJson());
		assertThat(repository.find(user.asIdentity()).toJson()).isEqualTo(user.toJson());
		assertThat(repository.exists(user.getName())).isTrue();
		assertThat(repository.isSuperuser(user.asIdentity())).isFalse();

		// update user
		user.setVerified(true);
		user.setSuperuser(true);
		repository.update(user);
		assertThat(repository.isSuperuser(user.asIdentity())).isTrue();
		assertThat(repository.find(user.getName()).toJson()).isEqualTo(user.toJson());

		// delete user
		repository.delete(user);
		// repository.refresh();
		assertThat(repository.find(user.getId())).isNull();
	}

	@Test
	public void testFindUsers() {

		List<User> users = newUserList(20);
		for (User user : users) {
			repository.store(user);
		}

		assertThat(repository.find(0, 10)).hasSize(users.size()).isEqualTo(users.subList(0, 10));
		assertThat(repository.find(10, 10)).hasSize(users.size()).isEqualTo(users.subList(10, 20));
	}

	@Test
	public void testScrollUsers() {

		List<User> users = newUserList(15); // large enough to require scrolling
		for (User user : users) {
			repository.store(user);
		}

		Callback<User> callback = mock(Callback.class);
		repository.find(callback);
		verify(callback, times(users.size())).call(any(User.class));
	}

	private static List<User> newUserList(int size) {
		Preconditions.checkArgument(size < 1000);
		List<User> users = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			users.add(new User(String.format("user%03d", i + 1)));
		}
		return users;
	}
}
