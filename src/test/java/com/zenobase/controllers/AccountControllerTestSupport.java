package com.zenobase.controllers;

import static org.mockito.Mockito.mock;

import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.BeforeEach;

import com.zenobase.mail.RegexEmailValidator;
import com.zenobase.mail.VerificationMailer;
import com.zenobase.models.User;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.BucketRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.CredentialsRepository;
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
	protected final User user = new User("tester");
	protected final String password = "secret123";

	@Override
	protected void routing(HttpRouting.Builder builder) {
		var controller = new AccountController(
				auth,
				users,
				buckets,
				tasks,
				credentials,
				authorizations,
				dispatcher,
				new RegexEmailValidator(),
				mailer);
		builder.post("/users/", controller::open);
		builder.delete("/users/{userId}", controller::close);
	}

	@BeforeEach
	public void setUp() {
		user.setEmail("jdoe@zenobase.com");
		user.setPassword(password);
	}
}
