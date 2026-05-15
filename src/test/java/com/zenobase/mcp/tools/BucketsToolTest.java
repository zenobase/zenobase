package com.zenobase.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.json.NodeList;
import com.zenobase.mcp.ConsentEnforcer;
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
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class BucketsToolTest {

	private final Identity user = new Identity("user-1");
	private final Identity clientId = new Identity("client-1");
	private final Authorization auth = new Authorization(user, clientId, "external");

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);

	private final BucketsTool tool = new BucketsTool(buckets, clients, enforcer);

	@Test
	public void testMetadata() {
		assertThat(tool.name()).isEqualTo("buckets");
		assertThat(tool.description()).contains("Zenobase buckets").contains("granted");
		assertThat(tool.inputSchema().get("type").asText()).isEqualTo("object");
	}

	@Test
	public void testReturnsOnlyReadableBuckets() {
		Bucket b1 = bucket("b1", "Weight");
		Bucket b2 = bucket("b2", "Sleep");
		Bucket b3 = bucket("b3", "Steps");
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(b1, b2, b3)
		);
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1", "b3"));

		JsonNode result = tool.call(auth, null);

		assertThat(result.get("buckets")).hasSize(2);
		assertThat(result.get("buckets").get(0).get("id").asText()).isEqualTo("b1");
		assertThat(result.get("buckets").get(0).get("label").asText()).isEqualTo("Weight");
		assertThat(result.get("buckets").get(1).get("id").asText()).isEqualTo("b3");
		assertThat(result.get("buckets").get(1).get("label").asText()).isEqualTo("Steps");
	}

	@Test
	public void testIncludesConsentMetaWhenNoGrants() {
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(bucket("b1", "Weight"))
		);
		when(clients.find(user, clientId)).thenReturn(
			connectedClient(
				/* none */
			)
		);
		when(enforcer.consentUrl()).thenReturn("https://zenobase.test/#/settings");

		JsonNode result = tool.call(auth, null);

		assertThat(result.get("buckets")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo("https://zenobase.test/#/settings");
	}

	@Test
	public void testIncludesConsentMetaWhenClientUnregistered() {
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(bucket("b1", "Weight"))
		);
		when(clients.find(user, clientId)).thenReturn(null);
		when(enforcer.consentUrl()).thenReturn("https://zenobase.test/#/settings");

		JsonNode result = tool.call(auth, null);

		assertThat(result.get("buckets")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo("https://zenobase.test/#/settings");
	}

	@Test
	public void testEmptyWhenTokenHasNoClient() {
		Authorization withoutClient = new Authorization(user, null, "external");
		when(enforcer.consentUrl()).thenReturn("https://zenobase.test/#/settings");

		JsonNode result = tool.call(withoutClient, null);

		assertThat(result.get("buckets")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo("https://zenobase.test/#/settings");
	}

	@Test
	public void testMarksArchivedBuckets() {
		Bucket archived = bucket("b1", "Old Weight");
		archived.setArchived(true);
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(archived)
		);
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1"));

		JsonNode result = tool.call(auth, null);

		assertThat(result.get("buckets").get(0).get("archived").asBoolean()).isTrue();
	}

	@Test
	public void testIncludesDescription() {
		Bucket withDesc = bucket("b1", "Weight");
		withDesc.setDescription("kg measurements");
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(withDesc)
		);
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1"));

		JsonNode result = tool.call(auth, null);

		assertThat(result.get("buckets").get(0).get("description").asText()).isEqualTo("kg measurements");
	}

	private Bucket bucket(String id, String label) {
		Bucket b = new Bucket(id);
		b.setLabel(label);
		b.addRole(user, Role.OWNER);
		return b;
	}

	private ExternalClient connectedClient(String... readableBuckets) {
		ExternalClient c = new ExternalClient(user, clientId, null, new DateTime(2026, 5, 1, 0, 0, DateTimeZone.UTC));
		c.setReadableBuckets(java.util.List.of(readableBuckets));
		return c;
	}

	private static BucketList bucketList(Bucket... values) {
		List<com.fasterxml.jackson.databind.node.ObjectNode> nodes = java.util.Arrays.stream(values)
			.map(Bucket::toJson)
			.toList();
		return new BucketList(new NodeList(nodes, values.length));
	}
}
