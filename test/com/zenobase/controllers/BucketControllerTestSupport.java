package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserRepository;

public abstract class BucketControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandQueue queue = mock(CommandQueue.class);
	protected final User user = new User(Generator.id(), "tester");

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(CommandQueue.class).toInstance(queue);
				requestStaticInjection(BucketController.class);
			}
		});
	}
}
