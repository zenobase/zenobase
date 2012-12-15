package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.models.Identity;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.UserRepository;

public abstract class QueueControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandRepository commands = mock(CommandRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final Identity principal = new Identity();

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(CommandRepository.class).toInstance(commands);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(QueueController.class).in(Singleton.class);
			}
		});
	}
}
