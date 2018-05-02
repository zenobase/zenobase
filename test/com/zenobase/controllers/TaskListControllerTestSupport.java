package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Singleton;
import play.test.FakeApplication;

import com.zenobase.models.User;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;
import com.zenobase.tasks.TaskManagerRegistry;

public abstract class TaskListControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final TaskManagerRegistry registry = mock(TaskManagerRegistry.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final User user = new User("tester");

	@Override
	protected FakeApplication provideFakeApplication() {
		return fakeApplication(new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(TaskManagerRegistry.class).toInstance(registry);
				bind(TaskRepository.class).toInstance(tasks);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(TaskListController.class).in(Singleton.class);
			}
		});
	}
}
