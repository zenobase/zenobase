package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import play.test.FakeApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.models.Identity;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.LocalBus;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.CredentialsManagerRegistry;

public abstract class CredentialsControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final CredentialsManagerRegistry registry = mock(CredentialsManagerRegistry.class);
	protected final CredentialsRepository repository = mock(CredentialsRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final Identity principal = new Identity();

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(CredentialsManagerRegistry.class).toInstance(registry);
				bind(CredentialsRepository.class).toInstance(repository);
				bind(UserRepository.class).toInstance(users);
				bind(CredentialsController.class).in(Singleton.class);
			}
		});
	}
}
