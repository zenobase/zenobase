package com.zenobase.repositories;

import static com.zenobase.testing.NodeAssert.assertThat;
import static com.zenobase.testing.PartialListAssert.assertThat;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.List;

import com.google.common.collect.Lists;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.zenobase.common.Callback;
import com.zenobase.models.User;
import com.zenobase.queries.UserQuery;

public class UserRepositoryTest extends OpenSearchTestSupport {

	private UserRepository repository;

	@BeforeEach
	public void setUp() {
		repository = new UserRepository(getManager());
	}

	@Test
	public void test() {

		// create user
		User user = new User("tester");
		user.setEmail("jdoe@zenobase.com");

		assertThat(repository.isEmpty()).isTrue();
		assertThat(repository.exists(user.getName())).isFalse();
		assertThat(repository.find(user.asIdentity())).isNull();
		assertThat(repository.delete(user)).isFalse();

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
		assertThat(repository.delete(user)).isTrue();
		assertThat(repository.find(user.getId())).isNull();
	}

	@Test
	public void testFindWithPaging() {
		List<User> users = fill(11);
		assertThat(repository.find(new UserQuery(), 0, 10))
				.hasTotal(users.size())
				.isEqualTo(users.subList(0, 10));
		assertThat(repository.find(new UserQuery(), 10, 10))
				.hasTotal(users.size())
				.isEqualTo(users.subList(10, 11));
		assertThat(repository.find(new UserQuery(), 20, 10))
				.hasTotal(users.size())
				.isEmpty();
	}

	@Test
	public void testFindWithCallback() {
		List<User> expected = fill(11);
		Callback<User> callback = mock(Callback.class);
		repository.find(callback);
		verifyInteractions(callback, expected);
	}

	private List<User> fill(int size) {
		List<User> users = Lists.newArrayListWithCapacity(size);
		for (int i = 0; i < size; ++i) {
			User user = new User(String.format("user%03d", i + 1));
			users.add(user);
			repository.store(user);
		}
		return users;
	}
}
