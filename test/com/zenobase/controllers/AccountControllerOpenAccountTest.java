package com.zenobase.controllers;

import static com.zenobase.test.ResultAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.*;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Guice;

import com.zenobase.commands.CreateUserCommand;
import com.zenobase.common.Generator;
import com.zenobase.models.User;
import com.zenobase.models.UserInfo;
import com.zenobase.services.BucketManager;
import com.zenobase.services.CommandQueue;
import com.zenobase.services.UserManager;

public class AccountControllerOpenAccountTest {

	private final SecurityContext auth = mock(SecurityContext.class);
	private final BucketManager buckets = mock(BucketManager.class);
	private final UserManager users = mock(UserManager.class);
	private final CommandQueue queue = mock(CommandQueue.class);
	private final VerificationMailer mailer = mock(VerificationMailer.class);
	private final User user = new User(Generator.id(), "tester");
	private final String password = "secret123";

	@Before
	public void setUp() {
		Guice.createInjector(new AbstractModule() {
			@Override
			protected void configure() {
				bind(SecurityContext.class).toInstance(auth);
				bind(BucketManager.class).toInstance(buckets);
				bind(UserManager.class).toInstance(users);
				bind(CommandQueue.class).toInstance(queue);
				bind(VerificationMailer.class).toInstance(mailer);
				requestStaticInjection(AccountController.class);
			}
		});
		user.setEmail("jdoe@zenobase.com");
		user.setPassword(password);
	}

	@Test
	public void testSignUp() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(CREATED).hasContent(new UserInfo(user).toJson());
		ArgumentCaptor<CreateUserCommand> commandArg = ArgumentCaptor.forClass(CreateUserCommand.class);
		verify(queue).dispatch(commandArg.capture());
		User actual = commandArg.getValue().getUser();
		assertThat(actual.getName()).isEqualTo(user.getName());
		assertThat(actual.getEmail()).isEqualTo(user.getEmail());
		ArgumentCaptor<User> userArg = ArgumentCaptor.forClass(User.class);
		verify(mailer).send(userArg.capture());
		assertThat(userArg.getValue()).isEqualTo(actual);
	}

	@Test
	public void testCantSignUpExistingUser() {
		String commandId = Generator.id();
		when(users.exists(user.getName())).thenReturn(true);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		when(queue.dispatch(any(CreateUserCommand.class))).thenReturn(commandId);
		SignUpForm form = new SignUpForm(user.getName(), password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(CONFLICT);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testCantSignUpAsGuest() {
		String username = "guest";
		when(users.exists(username)).thenReturn(false);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		SignUpForm form = new SignUpForm(username, password, user.getEmail());
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(queue, mailer);
	}

	@Test
	public void testCantSignUpWithInvalidData() {
		when(users.exists(user.getName())).thenReturn(false);
		when(auth.getPrincipal(true)).thenReturn(user.asIdentity());
		SignUpForm form = new SignUpForm(user.getName(), password, "x");
		Result result = call(form.toJson());
		assertThat(result).hasStatus(BAD_REQUEST);
		verifyZeroInteractions(queue, mailer);
	}

	private Result call(ObjectNode body) {
		return callAction(com.zenobase.controllers.routes.ref.AccountController.open(), fakeRequest().withJsonBody(body));
	}
}
