package com.zenobase.mcp.tools;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
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
import com.zenobase.search.Search;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

/**
 * Exercises the five facet/events tools against a real {@link EventRepository}-shaped mock and a stub
 * {@link ConsentEnforcer}. Goes through the real {@link ToolArgs}, {@link ToolSchemas}, and {@link ConstraintParser}
 * paths, so this is also the implicit coverage for those helpers.
 */
public class FacetToolsTest {

	private final Identity user = new Identity("user-1");
	private final Identity client = new Identity("client-1");
	private final Authorization auth = new Authorization(user, client, "external");
	private final Bucket bucket = bucket("b1");
	private final ObjectNode stubbedResult = Nodes.newObject("total", 7);

	private final EventRepository events = mock(EventRepository.class);
	private final ConsentEnforcer enforcer = mock(ConsentEnforcer.class);

	private final EventsTool eventsTool = new EventsTool(events, enforcer);
	private final HistogramTool histogramTool = new HistogramTool(events, enforcer);
	private final StatsTool statsTool = new StatsTool(events, enforcer);
	private final TermsTool termsTool = new TermsTool(events, enforcer);
	private final TimelineTool timelineTool = new TimelineTool(events, enforcer);

	public FacetToolsTest() {
		when(enforcer.requireRead(any(Authorization.class), eq("b1"))).thenReturn(bucket);
		when(events.find(eq("b1"), any(Search.class))).thenReturn(stubbedResult);
	}

	@Test
	public void testEventsHappyPath() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("limit", 25);

		JsonNode result = eventsTool.call(auth, args);

		assertThat(result).isSameAs(stubbedResult);
		verify(enforcer).requireRead(auth, "b1");
		verify(events).find(eq("b1"), any(Search.class));
	}

	@Test
	public void testEventsAcceptsOutOfRangeLimitAndOffset() {
		// Internal clamping (limit→[1,500], offset→[0,∞)) must keep the call from blowing up on adversarial input.
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("limit", 9999);
		args.put("offset", -10);

		assertThat(eventsTool.call(auth, args)).isSameAs(stubbedResult);
	}

	@Test
	public void testTermsHappyPath() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("field", "tag");
		args.put("limit", 999);

		assertThat(termsTool.call(auth, args)).isSameAs(stubbedResult);
		verify(events).find(eq("b1"), any(Search.class));
	}

	@Test
	public void testStatsHappyPath() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("field", "weight");

		assertThat(statsTool.call(auth, args)).isSameAs(stubbedResult);
	}

	@Test
	public void testHistogramHappyPath() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("field", "weight");
		args.put("interval", "10");

		assertThat(histogramTool.call(auth, args)).isSameAs(stubbedResult);
	}

	@Test
	public void testTimelineHappyPathUsesDefaultInterval() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");

		assertThat(timelineTool.call(auth, args)).isSameAs(stubbedResult);
	}

	@Test
	public void testMissingBucketIdRejected() {
		assertThatThrownBy(() -> eventsTool.call(auth, Nodes.newObject()))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("bucket_id");
	}

	@Test
	public void testStatsMissingFieldRejected() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		assertThatThrownBy(() -> statsTool.call(auth, args))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("field");
	}

	@Test
	public void testHistogramMissingIntervalRejected() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("field", "weight");
		assertThatThrownBy(() -> histogramTool.call(auth, args))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("interval");
	}

	@Test
	public void testTermsMissingFieldRejected() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		assertThatThrownBy(() -> termsTool.call(auth, args))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("field");
	}

	@Test
	public void testNonIntegerLimitRejected() {
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.put("limit", "ten");
		assertThatThrownBy(() -> eventsTool.call(auth, args))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("limit");
	}

	@Test
	public void testInvalidConstraintFieldWrappedAsInvalidParams() {
		// Triggers IllegalArgumentException from EventSearchBuilder.addConstraints — the FacetToolSupport / EventsTool
		// catch must surface it as McpException(INVALID_PARAMS) rather than letting the raw IAE escape.
		ObjectNode args = Nodes.newObject();
		args.put("bucket_id", "b1");
		args.set("constraints", Nodes.readArray("[{\"field\":\"no_such_field\",\"op\":\"eq\",\"value\":\"x\"}]"));

		assertThatThrownBy(() -> eventsTool.call(auth, args))
			.isInstanceOfSatisfying(McpException.class, e ->
				assertThat(e.getCode()).isEqualTo(McpException.INVALID_PARAMS)
			)
			.hasMessageContaining("Invalid query");
		assertThatThrownBy(() -> statsTool.call(auth, withField(args, "weight")))
			.isInstanceOf(McpException.class)
			.hasMessageContaining("Invalid query");
	}

	@Test
	public void testInputSchemasShape() {
		// Real ToolSchemas output: each tool advertises bucket_id + constraints, and the per-tool required fields.
		ObjectNode eventsSchema = eventsTool.inputSchema();
		assertThat(eventsSchema.get("type").asText()).isEqualTo("object");
		assertThat(eventsSchema.get("properties").has("bucket_id")).isTrue();
		assertThat(eventsSchema.get("properties").has("constraints")).isTrue();
		assertThat(eventsSchema.get("properties").has("limit")).isTrue();
		assertThat(requiredFields(eventsSchema)).containsExactly("bucket_id");

		assertThat(requiredFields(statsTool.inputSchema())).containsExactlyInAnyOrder("bucket_id", "field");
		assertThat(requiredFields(termsTool.inputSchema())).containsExactlyInAnyOrder("bucket_id", "field");
		assertThat(requiredFields(histogramTool.inputSchema())).containsExactlyInAnyOrder(
			"bucket_id",
			"field",
			"interval"
		);
		assertThat(requiredFields(timelineTool.inputSchema())).containsExactly("bucket_id");
		assertThat(timelineTool.inputSchema().get("properties").get("interval").get("enum"))
			.extracting(JsonNode::asText)
			.contains("month", "day", "year");
	}

	private static List<String> requiredFields(ObjectNode schema) {
		List<String> required = new ArrayList<>();
		schema.get("required").forEach(node -> required.add(node.asText()));
		return required;
	}

	private static ObjectNode withField(ObjectNode args, String field) {
		ObjectNode copy = args.deepCopy();
		copy.put("field", field);
		return copy;
	}

	private Bucket bucket(String id) {
		Bucket b = new Bucket(id);
		b.addRole(user, Role.OWNER);
		return b;
	}
}
