package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import io.helidon.webserver.http.HttpRouting;
import org.junit.Before;

import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
import com.zenobase.services.LocalBus;
import com.zenobase.services.PaymentGateway;
import com.zenobase.services.TaskRepository;
import com.zenobase.services.UserRepository;

public abstract class AccountControllerTestSupport extends ControllerTestSupport {

	protected final AuthorizationContext auth = mock(AuthorizationContext.class);
	protected final UserRepository users = mock(UserRepository.class);
	protected final BucketRepository buckets = mock(BucketRepository.class);
	protected final TaskRepository tasks = mock(TaskRepository.class);
	protected final CredentialsRepository credentials = mock(CredentialsRepository.class);
	protected final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	protected final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	protected final VerificationMailer mailer = mock(VerificationMailer.class);
	protected final PaymentGateway payments = mock(PaymentGateway.class);
	protected final User user = new User("tester");
	protected final String password = "secret123";

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
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(VerificationMailer.class).toInstance(mailer); // unused
				bind(PaymentGateway.class).toInstance(payments);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		AccountController controller = injector.getInstance(AccountController.class);
		builder.post("/users/", controller::open);
		builder.delete("/users/{userId}", controller::close);
	}

	@Before
	public void setUp() {
		user.setEmail("jdoe@zenobase.com");
		user.setPassword(password);
	}
}
