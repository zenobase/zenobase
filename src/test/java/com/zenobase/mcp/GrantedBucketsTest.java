package com.zenobase.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.NodeList;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.services.SearchOrder;
import java.util.Arrays;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

/**
 * Covers the shared pipeline behind {@code resources/list} and the {@code buckets} tool: how the calling
 * external client's grants narrow the user's bucket list, and when the empty-result branch surfaces a consent URL
 * for the model to relay.
 */
public class GrantedBucketsTest {

	private static final String CONSENT_URL = "https://zenobase.test/#/settings";

	private final Identity user = new Identity("user-1");
	private final Identity clientId = new Identity("client-1");
	private final Authorization auth = new Authorization(user, clientId, "external");

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);

	private final GrantedBuckets granted = new GrantedBuckets(buckets, clients, enforcer);

	public GrantedBucketsTest() {
		when(enforcer.consentUrl()).thenReturn(CONSENT_URL);
	}

	@Test
	public void testFiltersBucketsToGrantedSet() {
		Bucket b1 = bucket("b1");
		Bucket b2 = bucket("b2");
		Bucket b3 = bucket("b3");
		stubUserBuckets(b1, b2, b3);
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1", "b3"));

		GrantedBuckets.Result result = granted.list(auth);

		assertThat(result.buckets()).extracting(Bucket::getId).containsExactly("b1", "b3");
		assertThat(result.consentUrl()).isNull();
	}

	@Test
	public void testEmptyGrantsReturnsConsentUrl() {
		stubUserBuckets(bucket("b1"));
		when(clients.find(user, clientId)).thenReturn(connectedClient(/* none */));

		GrantedBuckets.Result result = granted.list(auth);

		assertThat(result.buckets()).isEmpty();
		assertThat(result.consentUrl()).isEqualTo(CONSENT_URL);
	}

	@Test
	public void testUnregisteredClientReturnsConsentUrl() {
		stubUserBuckets(bucket("b1"));
		when(clients.find(user, clientId)).thenReturn(null);

		GrantedBuckets.Result result = granted.list(auth);

		assertThat(result.buckets()).isEmpty();
		assertThat(result.consentUrl()).isEqualTo(CONSENT_URL);
	}

	@Test
	public void testTokenWithoutClientReturnsConsentUrl() {
		Authorization withoutClient = new Authorization(user, null, "external");

		GrantedBuckets.Result result = granted.list(withoutClient);

		assertThat(result.buckets()).isEmpty();
		assertThat(result.consentUrl()).isEqualTo(CONSENT_URL);
	}

	private void stubUserBuckets(Bucket... values) {
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(values)
		);
	}

	private Bucket bucket(String id) {
		Bucket b = new Bucket(id);
		b.addRole(user, Role.OWNER);
		return b;
	}

	private ExternalClient connectedClient(String... readableBuckets) {
		ExternalClient c = new ExternalClient(user, clientId, null, new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC));
		c.setReadableBuckets(List.of(readableBuckets));
		return c;
	}

	private static BucketList bucketList(Bucket... values) {
		List<ObjectNode> nodes = Arrays.stream(values).map(Bucket::toJson).toList();
		return new BucketList(new NodeList(nodes, values.length));
	}
}
