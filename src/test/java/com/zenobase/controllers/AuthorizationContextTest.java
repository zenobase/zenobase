package com.zenobase.controllers;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Base64;
import java.util.Optional;
import java.util.Set;

import io.helidon.http.HeaderNames;
import io.helidon.webserver.http.ServerRequest;
import org.junit.jupiter.api.Test;

import com.zenobase.auth.TokenValidator;
import com.zenobase.auth.local.LocalTokenService;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class AuthorizationContextTest {

	private static final String AUTH0_ISSUER = "https://tenant.auth0.com/";

	private final TokenValidator tokenValidator = mock(TokenValidator.class);
	private final LocalTokenService tokenService = new LocalTokenService("test-secret");

	public AuthorizationContextTest() {
		when(tokenValidator.issuer()).thenReturn(AUTH0_ISSUER);
	}

	private AuthorizationContext context(boolean withAuth0) {
		Set<TokenValidator> validators = withAuth0 ? Set.of(tokenService, tokenValidator) : Set.of(tokenService);
		return new AuthorizationContext(validators);
	}

	@Test
	public void testExtractToken() {
		assertThat(AuthorizationContext.extractToken("Bearer 1234567890abcdefg"))
				.isEqualTo("1234567890abcdefg");
		assertThat(AuthorizationContext.extractToken(null)).isNull();
		assertThat(AuthorizationContext.extractToken("Basic xyz")).isNull();
		assertThat(AuthorizationContext.extractToken("Bearer ")).isNull();
	}

	@Test
	public void testNoAuthorizationHeader() {
		ServerRequest request = mockRequest(null);

		assertThat(context(true).current(request)).isNull();
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	public void testNonJwtToken() {
		ServerRequest request = mockRequest("Bearer opaque-token-without-dots");

		assertThat(context(true).current(request)).isNull();
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	public void testLocalGuestToken() {
		String token = tokenService.createGuestToken();
		ServerRequest request = mockRequest("Bearer " + token);

		Authorization auth = context(true).current(request);

		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isNotNull();
		assertThat(auth.getScope()).isNull();
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	public void testLocalScopedToken() {
		Identity principal = new Identity("user1");
		Identity client = new Identity("client1");
		String token = tokenService.createScopedToken(principal, client, "bucket1");
		ServerRequest request = mockRequest("Bearer " + token);

		Authorization auth = context(true).current(request);

		assertThat(auth).isNotNull();
		assertThat(auth.getPrincipal()).isEqualTo(principal);
		assertThat(auth.getClient()).isEqualTo(client);
		assertThat(auth.getScope()).isEqualTo("bucket1");
		verify(tokenValidator, never()).validate(any());
	}

	@Test
	public void testAuth0Token() {
		Identity identity = new Identity("auth0-user");
		Authorization expected = new Authorization(identity);
		when(tokenValidator.validate(any())).thenReturn(expected);

		String fakeJwt = createFakeJwt(AUTH0_ISSUER);
		ServerRequest request = mockRequest("Bearer " + fakeJwt);

		Authorization auth = context(true).current(request);

		assertThat(auth).isSameAs(expected);
		verify(tokenValidator).validate(fakeJwt);
	}

	@Test
	public void testAuth0TokenValidationFailure() {
		when(tokenValidator.validate(any())).thenReturn(null);

		String fakeJwt = createFakeJwt(AUTH0_ISSUER);
		ServerRequest request = mockRequest("Bearer " + fakeJwt);

		assertThat(context(true).current(request)).isNull();
	}

	@Test
	public void testNoAuth0ValidatorConfigured() {
		String fakeJwt = createFakeJwt("https://unknown-issuer.com/");
		ServerRequest request = mockRequest("Bearer " + fakeJwt);

		assertThat(context(false).current(request)).isNull();
	}

	private static ServerRequest mockRequest(String authorizationHeader) {
		ServerRequest request = mock(ServerRequest.class, RETURNS_DEEP_STUBS);
		when(request.headers().first(HeaderNames.AUTHORIZATION))
				.thenReturn(authorizationHeader != null ? Optional.of(authorizationHeader) : Optional.empty());
		return request;
	}

	private static String createFakeJwt(String issuer) {
		String header = Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString("{\"alg\":\"RS256\",\"typ\":\"JWT\"}".getBytes());
		String payload = Base64.getUrlEncoder()
				.withoutPadding()
				.encodeToString(("{\"iss\":\"" + issuer + "\",\"sub\":\"user1\"}").getBytes());
		return header + "." + payload + ".fake-signature";
	}
}
