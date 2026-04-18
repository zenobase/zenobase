package com.zenobase.controllers;

import static com.zenobase.testing.ResultAssert.assertThat;
import static org.mockito.Mockito.*;

import com.google.inject.AbstractModule;
import com.google.inject.Injector;
import com.google.inject.Module;
import com.google.inject.Singleton;
import io.helidon.webclient.http1.Http1ClientResponse;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

import com.zenobase.common.DefaultPartialList;
import com.zenobase.common.PartialList;
import com.zenobase.models.User;
import com.zenobase.models.UserList;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.UserQuery;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.Bus;
import com.zenobase.services.LocalBus;

public class UserListControllerTest extends ControllerTestSupport {

	private final AuthorizationContext auth = mock(AuthorizationContext.class);
	private final UserRepository users = mock(UserRepository.class);
	private final User user = new User("tester");

	@Override
	protected Module module() {
		return new AbstractModule() {
			@Override
			protected void configure() {
				bind(Bus.class).to(LocalBus.class);
				bind(AuthorizationContext.class).toInstance(auth);
				bind(UserRepository.class).toInstance(users);
				bind(UserListController.class).in(Singleton.class);
			}
		};
	}

	@Override
	protected void routing(HttpRouting.Builder builder, Injector injector) {
		UserListController controller = injector.getInstance(UserListController.class);
		builder.get("/users/", controller::find);
	}

	@Test
	public void test() {
		PartialList<User> expected = DefaultPartialList.of();
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		when(users.find(new UserQuery().queryString("verified:true"), 0, 1)).thenReturn(expected);
		try (Http1ClientResponse result = call("verified:true", 0, 1)) {
			assertThat(result).hasStatus(200).hasContent(UserList.toJson(expected));
		}
	}

	@Test
	public void testNotAuthorized() {
		try (Http1ClientResponse result = call(null, 0, 1)) {
			assertThat(result).hasStatus(401);
		}
	}

	@Test
	public void testNotSuperuser() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		try (Http1ClientResponse result = call(null, 0, 1)) {
			assertThat(result).hasStatus(403);
		}
	}

	@Test
	public void testDownload() {
		when(auth.current(any())).thenReturn(new Authorization(user.asIdentity()));
		when(users.isSuperuser(user.asIdentity())).thenReturn(true);
		try (Http1ClientResponse result = call(null, 0, Integer.MAX_VALUE)) {
			assertThat(result).hasStatus(200).hasContentType("text/plain");
		}
	}

	private Http1ClientResponse call(String q, int offset, int limit) {
		var request = client
			.get("/users/")
			.queryParam("offset", String.valueOf(offset))
			.queryParam("limit", String.valueOf(limit));
		if (q != null) {
			request = request.queryParam("q", q);
		}
		return request.request();
	}
}
