package com.zenobase.filters;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import io.helidon.webserver.http.HttpRouting;
import org.junit.jupiter.api.Test;

public class LogContextFilterTest extends FilterTestSupport {

	private final AuthorizationContext authContext = mock(AuthorizationContext.class);

	@Override
	protected void configureFilters(HttpRouting.Builder routing) {
		routing.addFilter(new LogContextFilter(authContext));
	}

	@Test
	public void test() {
		when(authContext.current(any())).thenReturn(new Authorization(new Identity("user-1")));
		ping();
	}
}
