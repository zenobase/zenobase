package com.zenobase.mcp.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.zenobase.json.NodeList;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.models.BucketList;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import com.zenobase.services.SearchOrder;
import java.util.List;
import org.junit.jupiter.api.Test;

public class BucketResourceProviderTest {

	private final Identity user = new Identity("user-1");
	private final Identity client = new Identity("client-1");
	private final Authorization auth = new Authorization(user, client, "external");

	private final BucketRepository buckets = mock(BucketRepository.class);
	private final EventRepository events = mock(EventRepository.class);
	private final ExternalBucketGrantRepository grants = mock(ExternalBucketGrantRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);

	private final BucketResourceProvider provider = new BucketResourceProvider(buckets, events, grants, enforcer);

	@Test
	public void testListReturnsOnlyGrantedBuckets() {
		Bucket b1 = bucket("b1", "Weight");
		Bucket b2 = bucket("b2", "Sleep");
		Bucket b3 = bucket("b3", "Steps");
		when(buckets.find(any(BucketQuery.class), any(SearchOrder.class), anyInt(), anyInt())).thenReturn(
			bucketList(b1, b2, b3)
		);
		when(grants.grantedBuckets(user, client)).thenReturn(ImmutableSet.of("b1", "b3"));

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
		when(grants.grantedBuckets(user, client)).thenReturn(ImmutableSet.of());
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
		when(grants.grantedBuckets(user, client)).thenReturn(ImmutableSet.of("b1"));

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
		// payload is JSON in a text content block per MCP spec
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

	private static BucketList bucketList(Bucket... values) {
		List<ObjectNode> nodes = java.util.Arrays.stream(values).map(Bucket::toJson).toList();
		return new BucketList(new NodeList(nodes, values.length));
	}
}
