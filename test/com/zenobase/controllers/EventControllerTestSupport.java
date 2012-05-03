package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import org.junit.Before;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.common.Generator;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.User;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.IndexManager;

public abstract class EventControllerTestSupport {

	protected final SecurityContext auth = mock(SecurityContext.class);
	protected final BucketManager buckets = mock(BucketManager.class);
	protected final IndexManager indexes = mock(IndexManager.class);
	protected final CommandQueue queue = mock(CommandQueue.class);
	protected final User user = new User(Generator.id(), "tester");
	protected final Bucket bucket = new Bucket();
	protected final Event event = new Event();

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketManager.class).toInstance(buckets);
				bind(IndexManager.class).toInstance(indexes);
				bind(CommandQueue.class).toInstance(queue);
				requestStaticInjection(EventController.class);
			}
		});
	}
}
