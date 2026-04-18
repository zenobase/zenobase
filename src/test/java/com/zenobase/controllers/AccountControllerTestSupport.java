package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.zenobase.auth.UserDirectory;
import com.zenobase.models.User;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.LocalBus;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.BeforeEach;

public abstract class AccountControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final CredentialsRepository credentials = mock(CredentialsRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final UserDirectory userDirectory = mock(UserDirectory.class);
	protected final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(BucketRepository.class).toInstance(buckets);
				bind(UserRepository.class).toInstance(users);
				bind(TaskRepository.class).toInstance(tasks);
				bind(CredentialsRepository.class).toInstance(credentials);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(UserDirectory.class).toInstance(userDirectory);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		AccountController controller = injector.getInstance(AccountController.class);
		builder.delete("/users/{userId}", controller::close);
	}

	@BeforeEach
	public void setUp() {
		user.setEmail("jdoe@zenobase.com");
	}
}
