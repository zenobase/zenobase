package com.zenobase.filters;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zenobase.auth.auth0.Auth0TokenAuthorizer;
import com.zenobase.controllers.AuthorizationContext;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.ExternalClientRepository;
import io.helidon.common.uri.UriPath;
import io.helidon.http.HeaderName;
import io.helidon.http.HttpPrologue;
import io.helidon.http.Method;
import io.helidon.http.Status;
import io.helidon.webserver.http.FilterChain;
import io.helidon.webserver.http.RoutingRequest;
import io.helidon.webserver.http.RoutingResponse;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ExternalGrantFilterTest {

	private static final String WEB_HOSTNAME = "https://zenobase.test";

	private final AuthorizationContext authContext = mock(AuthorizationContext.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final ExternalGrantFilter filter = new ExternalGrantFilter(authContext, clients, WEB_HOSTNAME);

	private final Identity user = new Identity("user-1");
	private final Identity clientId = new Identity("client-1");

	// --- bucketIdFrom ---

	@Test
	public void testBucketIdFromExtractsId() {
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets/b1")).isEqualTo("b1");
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets/b1/")).isEqualTo("b1");
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets/b1/schema")).isEqualTo("b1");
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets/b1/tags/")).isEqualTo("b1");
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets/b1/e2")).isEqualTo("b1");
	}

	@Test
	public void testBucketIdFromReturnsNullForNonBucketPaths() {
		assertThat(ExternalGrantFilter.bucketIdFrom("/")).isNull();
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets/")).isNull();
		assertThat(ExternalGrantFilter.bucketIdFrom("/buckets")).isNull();
		assertThat(ExternalGrantFilter.bucketIdFrom("/mcp")).isNull();
		assertThat(ExternalGrantFilter.bucketIdFrom("/users/u1/buckets/")).isNull();
		assertThat(ExternalGrantFilter.bucketIdFrom("/who")).isNull();
	}

	// --- filter behavior ---

	@Test
	public void testFirstPartyTokenPassesThrough() {
		FilterChain chain = mock(FilterChain.class);
		when(authContext.current(any())).thenReturn(new Authorization(user)); // first-party, scope=null

		filter.filter(chain, request("GET", "/buckets/b1"), mock(RoutingResponse.class));

		verify(chain).proceed();
	}

	@Test
	public void testUnauthenticatedPassesThrough() {
		// Filter doesn't authenticate; downstream gates handle 401. The filter just doesn't apply.
		FilterChain chain = mock(FilterChain.class);
		when(authContext.current(any())).thenReturn(null);

		filter.filter(chain, request("GET", "/buckets/b1"), mock(RoutingResponse.class));

		verify(chain).proceed();
	}

	@Test
	public void testExternalTokenNonBucketPathPassesThrough() {
		FilterChain chain = mock(FilterChain.class);
		when(authContext.current(any())).thenReturn(externalAuth());

		filter.filter(chain, request("POST", "/mcp"), mock(RoutingResponse.class));
		filter.filter(chain, request("GET", "/who"), mock(RoutingResponse.class));
		filter.filter(chain, request("GET", "/users/u1/buckets/"), mock(RoutingResponse.class));

		verify(chain, times(3)).proceed();
	}

	@Test
	public void testExternalTokenGrantedBucketPassesThrough() {
		FilterChain chain = mock(FilterChain.class);
		when(authContext.current(any())).thenReturn(externalAuth());
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1", "b2"));

		filter.filter(chain, request("GET", "/buckets/b1/"), mock(RoutingResponse.class));
		filter.filter(chain, request("GET", "/buckets/b2/schema"), mock(RoutingResponse.class));

		verify(chain, times(2)).proceed();
	}

	@Test
	public void testExternalTokenUngrantedBucketForbidden() {
		FilterChain chain = mock(FilterChain.class);
		RoutingResponse res = mock(RoutingResponse.class);
		when(res.status(any(Status.class))).thenReturn(res);
		when(res.header(any(HeaderName.class), any(String[].class))).thenReturn(res);
		when(authContext.current(any())).thenReturn(externalAuth());
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1"));

		filter.filter(chain, request("GET", "/buckets/b2"), res);

		verify(chain, never()).proceed();
		verify(res).status(Status.FORBIDDEN_403);
	}

	@Test
	public void testExternalTokenUnregisteredClientForbidden() {
		FilterChain chain = mock(FilterChain.class);
		RoutingResponse res = mock(RoutingResponse.class);
		when(res.status(any(Status.class))).thenReturn(res);
		when(res.header(any(HeaderName.class), any(String[].class))).thenReturn(res);
		when(authContext.current(any())).thenReturn(externalAuth());
		when(clients.find(user, clientId)).thenReturn(null);

		filter.filter(chain, request("GET", "/buckets/b1"), res);

		verify(chain, never()).proceed();
		verify(res).status(Status.FORBIDDEN_403);
	}

	@Test
	public void testExternalTokenWriteOnBucketPathForbidden() {
		FilterChain chain = mock(FilterChain.class);
		RoutingResponse res = mock(RoutingResponse.class);
		when(res.status(any(Status.class))).thenReturn(res);
		when(res.header(any(HeaderName.class), any(String[].class))).thenReturn(res);
		when(authContext.current(any())).thenReturn(externalAuth());
		// readable_buckets contains b1, but writes are still forbidden
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1"));

		filter.filter(chain, request("POST", "/buckets/b1/"), res);
		filter.filter(chain, request("PUT", "/buckets/b1"), res);
		filter.filter(chain, request("DELETE", "/buckets/b1/e9"), res);

		verify(chain, never()).proceed();
		verify(res, times(3)).status(Status.FORBIDDEN_403);
	}

	@Test
	public void testExternalTokenWithoutClientForbidden() {
		FilterChain chain = mock(FilterChain.class);
		RoutingResponse res = mock(RoutingResponse.class);
		when(res.status(any(Status.class))).thenReturn(res);
		when(res.header(any(HeaderName.class), any(String[].class))).thenReturn(res);
		when(authContext.current(any())).thenReturn(new Authorization(user, null, Auth0TokenAuthorizer.EXTERNAL_SCOPE));

		filter.filter(chain, request("GET", "/buckets/b1"), res);

		verify(chain, never()).proceed();
		verify(res).status(Status.FORBIDDEN_403);
	}

	// --- helpers ---

	private Authorization externalAuth() {
		return new Authorization(user, clientId, Auth0TokenAuthorizer.EXTERNAL_SCOPE);
	}

	private ExternalClient connectedClient(String... readableBuckets) {
		ExternalClient c = new ExternalClient(user, clientId, null, new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC));
		c.setReadableBuckets(List.of(readableBuckets));
		return c;
	}

	private static RoutingRequest request(String method, String path) {
		RoutingRequest req = mock(RoutingRequest.class);
		HttpPrologue prologue = mock(HttpPrologue.class);
		when(req.prologue()).thenReturn(prologue);
		when(prologue.method()).thenReturn(Method.create(method));
		UriPath uriPath = mock(UriPath.class);
		when(prologue.uriPath()).thenReturn(uriPath);
		when(uriPath.rawPath()).thenReturn(path);
		return req;
	}
}
