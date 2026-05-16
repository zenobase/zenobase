package com.zenobase.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.repositories.EventRepository;
import java.util.List;
import org.junit.jupiter.api.Test;

public class SchemaToolTest {

	private final Identity user = new Identity("user-1");
	private final Authorization auth = new Authorization(user, new Identity("client-1"), "external");

	private final EventRepository events = mock(EventRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);

	private final SchemaTool tool = new SchemaTool(events, enforcer);

	@Test
	public void testMetadata() {
		assertThat(tool.name()).isEqualTo("schema");
		assertThat(tool.description()).contains("timestamp.hour_of_day").contains("constraints");
		ObjectNode schema = tool.inputSchema();
		assertThat(schema.get("type").asText()).isEqualTo("object");
		assertThat(schema.get("properties").has("bucket_id")).isTrue();
		assertThat(schema.get("required").get(0).asText()).isEqualTo("bucket_id");
	}

	@Test
	public void testReturnsSchemaPayload() {
		Bucket bucket = new Bucket("b1");
		bucket.setLabel("Weight");
		bucket.setDescription("kg measurements");
		bucket.addRole(user, Role.OWNER);
		when(enforcer.requireRead(auth, "b1")).thenReturn(bucket);
		when(events.fields("b1")).thenReturn(List.of());

		JsonNode result = tool.call(auth, args("b1"));

		assertThat(result.get("id").asText()).isEqualTo("b1");
		assertThat(result.get("label").asText()).isEqualTo("Weight");
		assertThat(result.get("description").asText()).isEqualTo("kg measurements");
		assertThat(result.has("archived")).isFalse();
		assertThat(result.has("schema")).isTrue();
	}

	@Test
	public void testMarksArchivedBucket() {
		Bucket bucket = new Bucket("b1");
		bucket.setArchived(true);
		bucket.addRole(user, Role.OWNER);
		when(enforcer.requireRead(auth, "b1")).thenReturn(bucket);
		when(events.fields("b1")).thenReturn(List.of());

		JsonNode result = tool.call(auth, args("b1"));

		assertThat(result.get("archived").asBoolean()).isTrue();
	}

	@Test
	public void testPropagatesEnforcerRejection() {
		when(enforcer.requireRead(auth, "b1")).thenThrow(
			new McpException(McpException.ACCESS_NOT_GRANTED, "No access to bucket: b1")
		);

		assertThatThrownBy(() -> tool.call(auth, args("b1")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("b1");
	}

	@Test
	public void testRejectsMissingBucketId() {
		assertThatThrownBy(() -> tool.call(auth, Nodes.newObject()))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("bucket_id");
	}

	private static ObjectNode args(String bucketId) {
		ObjectNode node = Nodes.newObject();
		node.put("bucket_id", bucketId);
		return node;
	}
}
