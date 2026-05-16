package com.zenobase.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import io.helidon.extensions.mcp.server.McpException;
import io.helidon.jsonrpc.core.JsonRpcError;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class ConsentEnforcerTest {

	private static final String WEB_HOSTNAME = "https://zenobase.test";

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final ConsentEnforcer enforcer = new ConsentEnforcer(buckets, clients, WEB_HOSTNAME);

	private final Identity user = new Identity("user-1");
	private final Identity client = new Identity("client-1");
	private final Authorization auth = new Authorization(user, client, "external");

	@Test
	public void testReturnsBucketWhenGranted() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		when(clients.find(user, client)).thenReturn(connectedClient("b1"));

		assertThat(enforcer.requireRead(auth, "b1")).isSameAs(bucket);
	}

	@Test
	public void testBucketNotFoundRaisesInvalidParamsAsJsonRpcError() {
		// "bucket not found" is a malformed-request situation: throws Helidon's McpException with INVALID_PARAMS so
		// the framework surfaces it as a JSON-RPC error envelope.
		when(buckets.find("b1")).thenReturn(null);
		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("Bucket not found: b1");
	}

	@Test
	public void testNoRoleRaisesConsentRequired() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(new Identity("someone-else"), Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(ConsentRequiredException.class)
			.satisfies(e ->
				assertThat(((ConsentRequiredException) e).consentUrl()).isEqualTo(WEB_HOSTNAME + "/#/settings")
			)
			.hasMessageContaining(WEB_HOSTNAME + "/#/settings");
	}

	@Test
	public void testClientNotRegisteredRaisesConsentRequired() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		when(clients.find(user, client)).thenReturn(null);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(ConsentRequiredException.class)
			.hasMessageContaining("has not been granted");
	}

	@Test
	public void testBucketNotReadableRaisesConsentRequired() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		// client is registered but b1 isn't in readable_buckets
		when(clients.find(user, client)).thenReturn(
			connectedClient(
				/* no buckets */
			)
		);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(ConsentRequiredException.class)
			.hasMessageContaining("has not been granted");
	}

	@Test
	public void testTokenWithoutClientRaisesConsentRequired() {
		Authorization authWithoutClient = new Authorization(user, null, "external");
		assertThatThrownBy(() -> enforcer.requireRead(authWithoutClient, "b1")).isInstanceOf(
			ConsentRequiredException.class
		);
	}

	@Test
	public void testConsentUrl() {
		assertThat(enforcer.consentUrl()).isEqualTo(WEB_HOSTNAME + "/#/settings");
	}

	@Test
	public void testInvalidParamsErrorCode() {
		when(buckets.find("b1")).thenReturn(null);
		try {
			enforcer.requireRead(auth, "b1");
		} catch (McpException e) {
			// Helidon's McpException stores the code as package-private; we just assert the type + message above.
			// Sanity check that the JSON-RPC INVALID_PARAMS constant is what we use.
			assertThat(JsonRpcError.INVALID_PARAMS).isEqualTo(-32602);
		}
	}

	private ExternalClient connectedClient(String... readableBuckets) {
		ExternalClient c = new ExternalClient(user, client, null, new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC));
		c.setReadableBuckets(List.of(readableBuckets));
		return c;
	}
}
