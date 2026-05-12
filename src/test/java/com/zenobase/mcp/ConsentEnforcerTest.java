package com.zenobase.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import org.junit.jupiter.api.Test;

public class ConsentEnforcerTest {

	private static final String WEB_HOSTNAME = "https://zenobase.test";

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final ExternalBucketGrantRepository grants = mock(ExternalBucketGrantRepository.class);
	private final ConsentEnforcer enforcer = new ConsentEnforcer(buckets, grants, WEB_HOSTNAME);

	private final Identity user = new Identity("user-1");
	private final Identity client = new Identity("client-1");
	private final Authorization auth = new Authorization(user, client, "external");

	@Test
	public void testReturnsBucketWhenGranted() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		when(grants.find(user, client, "b1")).thenReturn(new ExternalBucketGrant(user, client, "b1", "read"));

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
		// owned by someone else
		bucket.addRole(new Identity("someone-else"), Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED))
			.satisfies(e ->
				assertThat(((McpException) e).getData().get("consent_url").asText()).isEqualTo(
					WEB_HOSTNAME + "/settings/connected-apps"
				)
			);
	}

	@Test
	public void testNoGrantRaisesAccessNotGranted() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		when(grants.find(user, client, "b1")).thenReturn(null);

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED))
			.hasMessageContaining("has not been granted");
	}

	@Test
	public void testWrongRightsRaisesAccessNotGranted() {
		Bucket bucket = new Bucket("b1");
		bucket.addRole(user, Role.OWNER);
		when(buckets.find("b1")).thenReturn(bucket);
		when(grants.find(user, client, "b1")).thenReturn(new ExternalBucketGrant(user, client, "b1", "write"));

		assertThatThrownBy(() -> enforcer.requireRead(auth, "b1"))
			.isInstanceOf(McpException.class)
			.satisfies(e -> assertThat(((McpException) e).getCode()).isEqualTo(McpException.ACCESS_NOT_GRANTED))
			.hasMessageContaining("does not have read access");
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
		assertThat(enforcer.consentUrl()).isEqualTo(WEB_HOSTNAME + "/settings/connected-apps");
	}
}
