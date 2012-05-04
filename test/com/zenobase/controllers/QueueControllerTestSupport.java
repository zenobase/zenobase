package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.models.Identity;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.CommandStore;
import com.zenobase.services.UserManager;

public abstract class QueueControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final CommandStore commands = mock(CommandStore.class);
	protected final UserManager users = mock(UserManager.class);
	protected final CommandQueue queue = mock(CommandQueue.class);
	protected final Identity principal = new Identity();

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(CommandStore.class).toInstance(commands);
				bind(UserManager.class).toInstance(users);
				bind(CommandQueue.class).toInstance(queue);
				requestStaticInjection(QueueController.class);
			}
		});
	}
}
