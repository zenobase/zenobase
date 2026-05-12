package com.zenobase.mcp.resources;

import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.zenobase.common.PartialList;
import com.zenobase.json.JsonSchema;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.mcp.McpException;
import com.zenobase.models.Bucket;
import com.zenobase.models.Event;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.EventRepository;
import com.zenobase.repositories.ExternalClientRepository;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Translates a user's granted Zenobase buckets into MCP Resources. URI scheme is
 * {@code zenobase://bucket/{bucket_id}}. {@code resources/list} returns one entry per granted bucket;
 * {@code resources/read} returns the bucket's metadata plus its inferred schema (same payload as the existing
 * {@code GET /buckets/{id}/schema} REST endpoint).
 */
public class BucketResourceProvider {

	public static final String URI_PREFIX = "zenobase://bucket/";

	private static final int LIST_LIMIT = 500;

	private final BucketRepository buckets;
	private final EventRepository events;
	private final ExternalClientRepository clients;
	private final ConsentEnforcer enforcer;

	@Inject
	public BucketResourceProvider(
		BucketRepository buckets,
		EventRepository events,
		ExternalClientRepository clients,
		ConsentEnforcer enforcer
	) {
		this.buckets = buckets;
		this.events = events;
		this.clients = clients;
		this.enforcer = enforcer;
	}

	/** Returns the JSON-RPC {@code result} payload for {@code resources/list}. */
	public ObjectNode list(Authorization auth) {
		ObjectNode result = Nodes.newObject();
		ArrayNode array = result.putArray("resources");
		Identity clientId = auth.getClient();
		if (clientId == null) {
			// No client_id on the token — nothing we can match against grants. Return empty plus a consent URL hint.
			result.putObject("_meta").put("consent_url", enforcer.consentUrl());
			return result;
		}
		ExternalClient client = clients.find(auth.getPrincipal(), clientId);
		ImmutableSet<String> readable =
			client != null ? ImmutableSet.copyOf(client.getReadableBuckets()) : ImmutableSet.of();
		PartialList<Bucket> userBuckets = buckets.find(
			new BucketQuery().principalEqualTo(auth.getPrincipal()).includeArchived(true),
			BucketQuery.DEFAULT_ORDER,
			0,
			LIST_LIMIT
		);
		for (Bucket bucket : userBuckets) {
			if (!readable.contains(bucket.getId())) {
				continue;
			}
			array.add(toResource(bucket));
		}
		if (array.isEmpty()) {
			result.putObject("_meta").put("consent_url", enforcer.consentUrl());
		}
		return result;
	}

	/** Returns the JSON-RPC {@code result} payload for {@code resources/read}. */
	public ObjectNode read(Authorization auth, String uri) {
		String bucketId = parseBucketId(uri);
		Bucket bucket = enforcer.requireRead(auth, bucketId);
		ObjectNode content = Nodes.newObject();
		content.put("uri", uri);
		content.put("mimeType", "application/json");
		ObjectNode payload = Nodes.newObject();
		payload.put("@id", bucket.getId());
		if (bucket.getLabel() != null) {
			payload.put("label", bucket.getLabel());
		}
		if (bucket.getDescription() != null) {
			payload.put("description", bucket.getDescription());
		}
		payload.put("archived", bucket.isArchived());
		payload.set("schema", JsonSchema.forFields(events.fields(bucket.getId()), Event.READ_ONLY_FIELDS).toJson());
		content.put("text", payload.toString());

		ObjectNode result = Nodes.newObject();
		ArrayNode contents = result.putArray("contents");
		contents.add(content);
		return result;
	}

	private static String parseBucketId(@Nullable String uri) {
		if (uri == null || !uri.startsWith(URI_PREFIX)) {
			throw new McpException(McpException.INVALID_PARAMS, "Expected a " + URI_PREFIX + "* URI");
		}
		String id = uri.substring(URI_PREFIX.length());
		if (id.isBlank()) {
			throw new McpException(McpException.INVALID_PARAMS, "Missing bucket id in URI");
		}
		return id;
	}

	private static ObjectNode toResource(Bucket bucket) {
		ObjectNode node = Nodes.newObject();
		node.put("uri", URI_PREFIX + bucket.getId());
		node.put("name", bucket.getLabel() != null ? bucket.getLabel() : bucket.getId());
		List<String> description = new ArrayList<>();
		if (bucket.getDescription() != null) {
			description.add(bucket.getDescription());
		}
		if (bucket.isArchived()) {
			description.add("(archived)");
		}
		if (!description.isEmpty()) {
			node.put("description", String.join(" ", description));
		}
		node.put("mimeType", "application/json");
		return node;
	}
}
