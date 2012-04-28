package com.zenobase.services;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.Test;

import com.zenobase.common.Generator;
import com.zenobase.models.Identity;
import com.zenobase.models.User;

public class UserManagerTest extends ElasticSearchTestSupport {

	@Test
	public void test() {

		// create user
		Identity userId = new Identity(Generator.id());
		String name = "tester";
		DateTime created = new DateTime(DateTimeZone.UTC);
		User user = new User(userId.getId(), name, created);
		user.setEmail("jdoe@zenobase.com");
		user.setPassword("secret");

		IndexManager indexManager = mock(IndexManager.class);
		Index index = new Index(UserManager.INDEX_NAME, getClient());
		when(indexManager.getIndex(UserManager.INDEX_NAME)).thenReturn(index);
		UserManager manager = new UserManager(indexManager);

		// store and retrieve user
		manager.store(user);
		assertThat((Object) manager.find(userId).toJson()).isEqualTo(user.toJson());

		// update user
		user.setVerified(true);
		manager.update(user);
		assertThat((Object) manager.find(userId).toJson()).isEqualTo(user.toJson());


		// delete user
		manager.delete(user);
		index.refresh();
		assertThat(manager.find(userId)).as("user").isNull();
	}
}
