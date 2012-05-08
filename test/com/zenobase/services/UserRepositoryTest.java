package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.util.List;

import org.junit.Test;
import com.google.common.collect.ImmutableList;

import com.zenobase.models.User;

public class UserRepositoryTest extends ElasticSearchTestSupport {

	@Test
	public void testCrudUser() {

		// create user
		String name = "tester";
		User user = new User(name);
		user.setEmail("jdoe@zenobase.com");
		user.setPassword("secret123");

		UserRepository repository = new UserRepository(getManager());
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

		List<User> users = ImmutableList.of(
			new User("alice"), new User("bob"), new User("carol"),
			new User("dave"), new User("eve"));

		UserRepository repository = new UserRepository(getManager());
		assertThat(repository.isEmpty()).isTrue();
		for (User user : users) {
			repository.store(user);
		}

		assertThat(repository.find(0, 10)).hasSize(users.size()).isEqualTo(users);
		assertThat(repository.find(1, 2)).hasSize(users.size()).isEqualTo(users.subList(1, 3));
	}
}
