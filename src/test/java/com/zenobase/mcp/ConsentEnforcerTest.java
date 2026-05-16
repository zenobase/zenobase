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
	public void testBucketNotFoundRaisesInvalidParams() {
		when(buckets.find("b1")).thenReturn(null);
		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.INVALID_PARAMS));
	}

	@Test
	public void testNoRoleRaisesAccessNotGranted() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(new Identity("someone-else"), Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED))
			.satisfies(e ->
				assertThat(((McpException) e).getData().get("consent_url").asText()).isEqualTo(
					WEB_HOSTNAME + "/#/settings"
				)
			);
	}

	@Test
	public void testClientNotRegisteredRaisesAccessNotGranted() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		when(clients.find(user, client)).thenReturn(null);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED))
			.hasMessageContaining("has not been granted");
	}

	@Test
	public void testBucketNotReadableRaisesAccessNotGranted() {
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
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED))
			.hasMessageContaining("has not been granted");
	}

	@Test
	public void testTokenWithoutClientRaisesAccessNotGranted() {
		Authorization authWithoutClient = new Authorization(user, null, "external");
		assertThatThrownBy(() -> enforcer.requireRead(authWithoutClient, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED));
	}

	@Test
	public void testConsentUrl() {
		assertThat(enforcer.consentUrl()).isEqualTo(WEB_HOSTNAME + "/#/settings");
	}

	private ExternalClient connectedClient(String... readableBuckets) {
		ExternalClient c = new ExternalClient(user, client, null, new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC));
		c.setReadableBuckets(List.of(readableBuckets));
		return c;
	}
}
