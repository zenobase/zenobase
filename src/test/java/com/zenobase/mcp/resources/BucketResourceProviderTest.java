package com.zenobase.mcp.resources;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.GrantedBuckets;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * The shared {@code resources/list} pipeline (readable-set filter, consent-hint emission, unregistered client
 * handling) lives in {@link GrantedBuckets} and is tested by {@link com.zenobase.mcp.GrantedBucketsTest}. This
 * test covers the resource-specific concerns: the URI / description / archived-suffix formatter for
 * {@code resources/list}, and the {@code resources/read} flow (URI parsing, consent enforcement, schema payload).
 */
public class BucketResourceProviderTest {

	private final Identity user = new Identity("user-1");
	private final Authorization auth = new Authorization(user, new Identity("client-1"), "external");

	private final EventRepository events = mock(EventRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);
	private final GrantedBuckets granted = mock(GrantedBuckets.class);

	private final BucketResourceProvider provider = new BucketResourceProvider(events, enforcer, granted);

	@Test
	public void testListFormatsGrantedBucketsAsResources() {
		Bucket weight = bucket("b1", "Weight");
		Bucket archived = bucket("b2", null);
		archived.setDescription("step counts");
		archived.setArchived(true);
		when(granted.list(auth)).thenReturn(new GrantedBuckets.Result(List.of(weight, archived), null));

		ObjectNode result = provider.list(auth);

		assertThat(result.has("_meta")).isFalse();
		assertThat(result.get("resources")).hasSize(2);
		var first = result.get("resources").get(0);
		assertThat(first.get("uri").asText()).isEqualTo("zenobase://bucket/b1");
		assertThat(first.get("name").asText()).isEqualTo("Weight");
		assertThat(first.get("mimeType").asText()).isEqualTo("application/json");
		var second = result.get("resources").get(1);
		// Falls back to id when label is null; archived appends "(archived)" to description.
		assertThat(second.get("name").asText()).isEqualTo("b2");
		assertThat(second.get("description").asText()).isEqualTo("step counts (archived)");
	}

	@Test
	public void testListEmitsConsentHintWhenGrantedReturnsConsentUrl() {
		when(granted.list(auth)).thenReturn(new GrantedBuckets.Result(List.of(), "https://zenobase.test/#/settings"));

		ObjectNode result = provider.list(auth);

		assertThat(result.get("resources")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo("https://zenobase.test/#/settings");
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
		if (label != null) {
			b.setLabel(label);
		}
		b.addRole(user, Role.OWNER);
		return b;
	}
}
