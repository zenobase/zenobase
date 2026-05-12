package com.zenobase.mcp.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.NodeList;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.services.SearchOrder;
import java.util.List;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.junit.jupiter.api.Test;

public class BucketResourceProviderTest {

	private final Identity user = new Identity("user-1");
	private final Identity clientId = new Identity("client-1");
	private final Authorization auth = new Authorization(user, clientId, "external");

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final EventRepository events = mock(EventRepository.class);
	private final ExternalClientRepository clients = mock(ExternalClientRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);

	private final BucketResourceProvider provider = new BucketResourceProvider(buckets, events, clients, enforcer);

	@Test
	public void testListReturnsOnlyReadableBuckets() {
		Bucket b1 = bucket("b1", "Weight");
		Bucket b2 = bucket("b2", "Sleep");
		Bucket b3 = bucket("b3", "Steps");
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(b1, b2, b3)
		);
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1", "b3"));

		ObjectNode result = provider.list(auth);

		assertThat(result.get("resources")).hasSize(2);
		assertThat(result.get("resources").get(0).get("uri").asText()).isEqualTo("zenobase://bucket/b1");
		assertThat(result.get("resources").get(0).get("name").asText()).isEqualTo("Weight");
		assertThat(result.get("resources").get(1).get("uri").asText()).isEqualTo("zenobase://bucket/b3");
	}

	@Test
	public void testListIncludesConsentMetaWhenNoGrants() {
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(bucket("b1", "Weight"))
		);
		when(clients.find(user, clientId)).thenReturn(
			connectedClient(
				/* none */
			)
		);
		when(enforcer.consentUrl()).thenReturn("https://zenobase.test/settings/connected-apps");

		ObjectNode result = provider.list(auth);

		assertThat(result.get("resources")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo(
			"https://zenobase.test/settings/connected-apps"
		);
	}

	@Test
	public void testListIncludesConsentMetaWhenClientUnregistered() {
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(bucket("b1", "Weight"))
		);
		when(clients.find(user, clientId)).thenReturn(null);
		when(enforcer.consentUrl()).thenReturn("https://zenobase.test/settings/connected-apps");

		ObjectNode result = provider.list(auth);

		assertThat(result.get("resources")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo(
			"https://zenobase.test/settings/connected-apps"
		);
	}

	@Test
	public void testListMarksArchivedBuckets() {
		Bucket archived = bucket("b1", "Old Weight");
		archived.setArchived(true);
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(archived)
		);
		when(clients.find(user, clientId)).thenReturn(connectedClient("b1"));

		ObjectNode result = provider.list(auth);

		assertThat(result.get("resources").get(0).get("description").asText()).contains("(archived)");
	}

	@Test
	public void testListEmptyWhenTokenHasNoClient() {
		Authorization withoutClient = new Authorization(user, null, "external");
		when(enforcer.consentUrl()).thenReturn("https://zenobase.test/settings/connected-apps");

		ObjectNode result = provider.list(withoutClient);

		assertThat(result.get("resources")).isEmpty();
	}

	@Test
	public void testReadDelegatesToEnforcerAndReturnsSchema() {
		Bucket bucket = bucket("b1", "Weight");
		bucket.setDescription("kg measurements");
		when(enforcer.requireRead(auth, "b1")).thenReturn(bucket);
		when(events.fields("b1")).thenReturn(List.of());

		ObjectNode result = provider.read(auth, "zenobase://bucket/b1");

		assertThat(result.get("contents")).hasSize(1);
		var content = result.get("contents").get(0);
		assertThat(content.get("uri").asText()).isEqualTo("zenobase://bucket/b1");
		assertThat(content.get("mimeType").asText()).isEqualTo("application/json");
		String text = content.get("text").asText();
		assertThat(text).contains("\"@id\":\"b1\"");
		assertThat(text).contains("\"label\":\"Weight\"");
		assertThat(text).contains("\"description\":\"kg measurements\"");
		assertThat(text).contains("\"schema\"");
	}

	@Test
	public void testReadRejectsMalformedUri() {
		assertThatThrownBy(() -> provider.read(auth, "wrong://bucket/b1"))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("zenobase://bucket/");
	}

	@Test
	public void testReadRejectsEmptyBucketId() {
		assertThatThrownBy(() -> provider.read(auth, "zenobase://bucket/"))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("Missing bucket id");
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
		List<ObjectNode> nodes = java.util.Arrays.stream(values).map(Bucket::toJson).toList();
		return new BucketList(new NodeList(nodes, values.length));
	}
}
