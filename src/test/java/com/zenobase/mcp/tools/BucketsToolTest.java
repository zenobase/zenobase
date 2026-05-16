package com.zenobase.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.zenobase.mcp.GrantedBuckets;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Pipeline behavior (readable-set filter, consent-hint emission, unregistered client handling) lives in
 * {@link GrantedBuckets} and is tested by {@link com.zenobase.mcp.GrantedBucketsTest}. This test only covers
 * the {@link BucketsTool}-specific formatter that turns the granted-buckets result into the tool response shape.
 */
public class BucketsToolTest {

	private final Identity user = new Identity("user-1");
	private final Authorization auth = new Authorization(user, new Identity("client-1"), "external");
	private final GrantedBuckets granted = mock(GrantedBuckets.class);
	private final BucketsTool tool = new BucketsTool(granted);

	@Test
	public void testMetadata() {
		assertThat(tool.name()).isEqualTo("buckets");
		assertThat(tool.description()).contains("Zenobase buckets").contains("granted");
		assertThat(tool.inputSchema().get("type").asText()).isEqualTo("object");
	}

	@Test
	public void testFormatsGrantedBuckets() {
		Bucket weight = bucket("b1", "Weight");
		weight.setDescription("kg measurements");
		Bucket archived = bucket("b2", "Old Steps");
		archived.setArchived(true);
		when(granted.list(auth)).thenReturn(new GrantedBuckets.Result(List.of(weight, archived), null));

		JsonNode result = tool.call(auth, null);

		assertThat(result.has("_meta")).isFalse();
		assertThat(result.get("buckets")).hasSize(2);
		JsonNode first = result.get("buckets").get(0);
		assertThat(first.get("id").asText()).isEqualTo("b1");
		assertThat(first.get("label").asText()).isEqualTo("Weight");
		assertThat(first.get("description").asText()).isEqualTo("kg measurements");
		assertThat(first.has("archived")).isFalse();
		assertThat(result.get("buckets").get(1).get("archived").asBoolean()).isTrue();
	}

	@Test
	public void testEmitsConsentHintWhenGrantedReturnsConsentUrl() {
		when(granted.list(auth)).thenReturn(new GrantedBuckets.Result(List.of(), "https://zenobase.test/#/settings"));

		JsonNode result = tool.call(auth, null);

		assertThat(result.get("buckets")).isEmpty();
		assertThat(result.get("_meta").get("consent_url").asText()).isEqualTo("https://zenobase.test/#/settings");
	}

	private Bucket bucket(String id, String label) {
		Bucket b = new Bucket(id);
		b.setLabel(label);
		b.addRole(user, Role.OWNER);
		return b;
	}
}
