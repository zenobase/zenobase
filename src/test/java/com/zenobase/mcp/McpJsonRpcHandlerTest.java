package com.zenobase.mcp;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.IntNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.resources.BucketResourceProvider;
import com.zenobase.mcp.tools.McpTool;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import java.util.Set;
import org.junit.jupiter.api.Test;

/** Exercises the JSON-RPC envelope handling and method dispatch in {@link McpJsonRpcHandler}. */
public class McpJsonRpcHandlerTest {

	private final Authorization auth = new Authorization(new Identity("user-1"), new Identity("client-1"), "external");
	private final BucketResourceProvider buckets = mock(BucketResourceProvider.class);

	@Test
	public void testInitializeAdvertisesCapabilities() {
		McpJsonRpcHandler handler = handler();
		ObjectNode response = handler.handle(auth, request(1, "initialize", null));
		assertThat(response).isNotNull();
		JsonNode result = response.get("result");
		assertThat(result.get("protocolVersion").asText()).isEqualTo("2025-06-18");
		assertThat(result.get("serverInfo").get("name").asText()).isEqualTo("zenobase");
		assertThat(result.get("capabilities").has("tools")).isTrue();
		assertThat(result.get("capabilities").has("resources")).isTrue();
	}

	@Test
	public void testToolsListIncludesRegisteredTool() {
		McpTool tool = mockTool("events", "list events");
		McpJsonRpcHandler handler = handler(tool);
		ObjectNode response = handler.handle(auth, request(2, "tools/list", null));
		assertThat(response).isNotNull();
		assertThat(response.get("result").get("tools")).hasSize(1);
		assertThat(response.get("result").get("tools").get(0).get("name").asText()).isEqualTo("events");
	}

	@Test
	public void testToolsCallInvokesNamedTool() {
		McpTool tool = mockTool("events", "list events");
		when(tool.call(eq(auth), any())).thenReturn(Nodes.newObject("total", 7));
		McpJsonRpcHandler handler = handler(tool);

		ObjectNode params = Nodes.newObject();
		params.put("name", "events");
		params.set("arguments", Nodes.newObject("bucket_id", "b1"));

		ObjectNode response = handler.handle(auth, request(3, "tools/call", params));
		assertThat(response).isNotNull();
		assertThat(response.get("result").get("structuredContent").get("data").get("total").asInt()).isEqualTo(7);
		verify(tool).call(eq(auth), any());
	}

	@Test
	public void testToolsCallUnknownToolErrors() {
		McpJsonRpcHandler handler = handler();
		ObjectNode params = Nodes.newObject();
		params.put("name", "nope");

		ObjectNode response = handler.handle(auth, request(4, "tools/call", params));
		assertThat(response).isNotNull();
		assertThat(response.get("error").get("code").asInt()).isEqualTo(McpException.METHOD_NOT_FOUND);
	}

	@Test
	public void testResourcesListDelegatesToProvider() {
		ObjectNode expected = Nodes.newObject("resources", Nodes.MAPPER.createArrayNode().toString());
		when(buckets.list(auth)).thenReturn(expected);
		McpJsonRpcHandler handler = handler();
		ObjectNode response = handler.handle(auth, request(5, "resources/list", null));
		assertThat(response).isNotNull();
		assertThat(response.get("result")).isEqualTo(expected);
	}

	@Test
	public void testResourcesReadDelegatesToProvider() {
		ObjectNode expected = Nodes.newObject();
		when(buckets.read(auth, "zenobase://bucket/b1")).thenReturn(expected);
		McpJsonRpcHandler handler = handler();
		ObjectNode params = Nodes.newObject();
		params.put("uri", "zenobase://bucket/b1");
		ObjectNode response = handler.handle(auth, request(6, "resources/read", params));
		assertThat(response).isNotNull();
		assertThat(response.get("result")).isEqualTo(expected);
	}

	@Test
	public void testResourcesReadWithoutUriErrors() {
		McpJsonRpcHandler handler = handler();
		ObjectNode response = handler.handle(auth, request(7, "resources/read", Nodes.newObject()));
		assertThat(response).isNotNull();
		assertThat(response.get("error").get("code").asInt()).isEqualTo(McpException.INVALID_PARAMS);
	}

	@Test
	public void testUnknownMethodReturnsErrorEnvelope() {
		McpJsonRpcHandler handler = handler();
		ObjectNode response = handler.handle(auth, request(8, "fake/method", null));
		assertThat(response).isNotNull();
		assertThat(response.get("error").get("code").asInt()).isEqualTo(McpException.METHOD_NOT_FOUND);
		assertThat(response.get("error").get("message").asText()).contains("fake/method");
		assertThat(response.get("id")).isEqualTo(new IntNode(8));
	}

	@Test
	public void testNotificationsProduceNoResponse() {
		McpJsonRpcHandler handler = handler();
		ObjectNode req = Nodes.newObject();
		req.put("jsonrpc", "2.0");
		req.put("method", "notifications/initialized");
		assertThat(handler.handle(auth, req)).isNull();
	}

	@Test
	public void testNonObjectRequestProducesErrorEnvelope() {
		McpJsonRpcHandler handler = handler();
		ObjectNode response = handler.handle(auth, new TextNode("garbage"));
		assertThat(response).isNotNull();
		assertThat(response.get("error").get("code").asInt()).isEqualTo(McpException.INVALID_PARAMS);
	}

	@Test
	public void testPingReturnsEmptyResult() {
		McpJsonRpcHandler handler = handler();
		ObjectNode response = handler.handle(auth, request(9, "ping", null));
		assertThat(response).isNotNull();
		assertThat(response.get("result")).isNotNull();
		assertThat(response.get("error")).isNull();
	}

	private McpJsonRpcHandler handler(McpTool... tools) {
		return new McpJsonRpcHandler(Set.of(tools), buckets);
	}

	private static McpTool mockTool(String name, String description) {
		McpTool tool = mock(McpTool.class);
		when(tool.name()).thenReturn(name);
		when(tool.description()).thenReturn(description);
		when(tool.inputSchema()).thenReturn(Nodes.newObject());
		return tool;
	}

	private static ObjectNode request(int id, String method, JsonNode params) {
		ObjectNode req = Nodes.newObject();
		req.put("jsonrpc", "2.0");
		req.put("id", id);
		req.put("method", method);
		if (params != null) {
			req.set("params", params);
		}
		return req;
	}
}
