package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.UserRepository;

public abstract class UserListControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final User user = new User(Generator.id(), "tester");

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				requestStaticInjection(UserListController.class);
			}
		});
	}
}
