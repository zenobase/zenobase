package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import play.test.FakeApplication;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.services.Cluster;
import com.zenobase.services.CommandRepository;
import com.zenobase.services.HazelcastManager;
import com.zenobase.services.IndexManager;

public abstract class StatusControllerTestSupport extends ControllerTestSupport {

	protected final IndexManager manager = mock(IndexManager.class);
	protected final Cluster cluster = mock(Cluster.class);
	protected final CommandRepository history = mock(CommandRepository.class);
	protected final HazelcastManager hazelcast = mock(HazelcastManager.class);

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(mock(AuthorizationContext.class));
				bind(IndexManager.class).toInstance(manager);
				bind(CommandRepository.class).toInstance(history);
				bind(HazelcastManager.class).toInstance(hazelcast);
				bind(StatusController.class).in(Singleton.class);
			}
		});
	}
}
