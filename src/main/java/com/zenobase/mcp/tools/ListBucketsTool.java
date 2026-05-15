package com.zenobase.mcp.tools;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;
import com.zenobase.mcp.ConsentEnforcer;
import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.ExternalClientRepository;
import jakarta.inject.Inject;

/**
 * Lists the buckets the calling external client has been granted access to. Exists alongside the {@code resources/list}
 * provider in {@link com.zenobase.mcp.resources.BucketResourceProvider} because Claude (and likely other LLM MCP
 * surfaces) do not autonomously invoke {@code resources/list} from conversational context — they treat resources as
 * user-attached items rather than model-discoverable catalogs. Exposing the same data as a tool means the model can
 * call it when the user says something like "use Zenobase" without first knowing a {@code bucket_id}.
 *
 * <p>Same security boundary as the resource provider: filters to buckets in the calling client's {@code readable_buckets}
 * grant. No extra surface area to harden.
 */
public class ListBucketsTool implements McpTool {

	private static final int LIST_LIMIT = 500;

	private final BucketRepository buckets;
	private final ExternalClientRepository clients;
	private final ConsentEnforcer enforcer;

	@Inject
	public ListBucketsTool(BucketRepository buckets, ExternalClientRepository clients, ConsentEnforcer enforcer) {
		this.buckets = buckets;
		this.clients = clients;
		this.enforcer = enforcer;
	}

	@Override
	public String name() {
		return "list_buckets";
	}

	@Override
	public String description() {
		return (
			"Lists the Zenobase buckets the user has granted this client access to. Returns id, label, description, " +
			"and archived flag for each. Call this first when the user references Zenobase data without specifying a " +
			"particular bucket, so subsequent calls to events/histogram/stats/terms/timeline can pass the right bucket_id."
		);
	}

	@Override
	public ObjectNode inputSchema() {
		ObjectNode schema = Nodes.newObject();
		schema.put("type", "object");
		schema.putObject("properties");
		return schema;
	}

	@Override
	public JsonNode call(Authorization auth, JsonNode args) {
		ObjectNode result = Nodes.newObject();
		ArrayNode array = result.putArray("buckets");
		Identity clientId = auth.getClient();
		if (clientId == null) {
			// No client_id on the token — nothing we can match against grants. Mirror BucketResourceProvider.list's
			// shape: empty list + consent_url hint so the model can guide the user toward Settings.
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
			array.add(toJson(bucket));
		}
		if (array.isEmpty()) {
			result.putObject("_meta").put("consent_url", enforcer.consentUrl());
		}
		return result;
	}

	private static ObjectNode toJson(Bucket bucket) {
		ObjectNode node = Nodes.newObject();
		node.put("id", bucket.getId());
		if (bucket.getLabel() != null) {
			node.put("label", bucket.getLabel());
		}
		if (bucket.getDescription() != null) {
			node.put("description", bucket.getDescription());
		}
		if (bucket.isArchived()) {
			node.put("archived", true);
		}
		return node;
	}
}
