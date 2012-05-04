package com.zenobase.services;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class UserRepositoryTest extends ElasticSearchTestSupport {

	@Test
	public void test() {

		// create user
		Identity userId = new Identity(Generator.id());
		String name = "tester";
		User user = new User(userId.getId(), name);
		user.setEmail("jdoe@zenobase.com");
		user.setPassword("secret");

		IndexManager indexManager = mock(IndexManager.class);
		Index index = new Index(UserRepository.INDEX_NAME, getClient());
		when(indexManager.getIndex(UserRepository.INDEX_NAME)).thenReturn(index);
		UserRepository repository = new UserRepository(indexManager);
		assertThat(repository.isEmpty()).as("no users").isTrue();

		// store and retrieve user
		repository.store(user);
		assertThat(repository.find(userId).toJson()).isEqualTo(user.toJson());

		// update user
		user.setVerified(true);
		repository.update(user);
		assertThat(repository.find(userId).toJson()).isEqualTo(user.toJson());

		// delete user
		repository.delete(user);
		index.refresh();
		assertThat(repository.find(userId)).as("user").isNull();
	}
}
