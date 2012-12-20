package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;
import static play.mvc.Http.Status.*;
import static play.test.Helpers.callAction;

import org.junit.Before;
import org.junit.Test;
import play.mvc.Result;
import com.google.inject.AbstractModule;
import com.google.inject.Singleton;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.oauth.AuthorizationList;
import com.zenobase.services.AuthorizationRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserRepository;

public class AuthorizationListControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final AuthorizationRepository authorizations = mock(AuthorizationRepository.class);
	private final UserRepository users = mock(UserRepository.class);
	private final CommandDispatcher dispatcher = mock(CommandDispatcher.class);
	private final User user = new User("tester");

	@Before
	public void setUp() {
		start(new AbstractModule() {
			@Override
			protected void configure() {
				bind(AuthorizationContext.class).toInstance(auth);
				bind(AuthorizationRepository.class).toInstance(authorizations);
				bind(UserRepository.class).toInstance(users);
				bind(CommandDispatcher.class).toInstance(dispatcher);
				bind(AuthorizationController.class).in(Singleton.class);
			}
		});
	}

	@Test
	public void testFindByPrincipal() {
		PartialList<Authorization> list = new DefaultPartialList<Authorization>();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(authorizations.find(Authorization.PRINCIPAL.getName(), user.asIdentity().toString(), false, 0, 10)).thenReturn(list);
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testFindByOtherPrincipal() {
		when(auth.current()).thenReturn(new Authorization(new Identity()));
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testFindAllAsSuperuser() {
		PartialList<Authorization> list = new DefaultPartialList<Authorization>();
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(authorizations.find(0, 10)).thenReturn(list);
		Result result = call(null, false, 0, 10);
		assertThat(result).hasStatus(OK).hasContent(AuthorizationList.toJson(list));
	}

	@Test
	public void testFindAll() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(null, false, 0, 10);
		assertThat(result).hasStatus(FORBIDDEN);
	}

	@Test
	public void testUnauthorized() {
		when(auth.current()).thenReturn(null);
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, 10);
		assertThat(result).hasStatus(UNAUTHORIZED);
	}

	@Test
	public void testBadLimit() {
		when(auth.current()).thenReturn(new Authorization(user.asIdentity()));
		Result result = call(Authorization.PRINCIPAL.getName() + ":" + user.asIdentity(), false, 0, Integer.MAX_VALUE);
		assertThat(result).hasStatus(BAD_REQUEST);
	}

	private static Result call(String query, boolean clientOnly, int offset, int limit) {
		return callAction(com.zenobase.controllers.routes.ref.AuthorizationListController.find(query, clientOnly, offset, limit));
	}
}
