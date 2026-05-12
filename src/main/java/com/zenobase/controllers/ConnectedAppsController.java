package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.zenobase.commands.CreateExternalBucketGrantCommand;
import com.zenobase.commands.DeleteExternalBucketGrantCommand;
import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;
import com.zenobase.models.ExternalBucketGrant;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.repositories.ExternalBucketGrantRepository;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.HashSet;
import java.util.Set;
import org.jspecify.annotations.Nullable;

/**
 * Lets a user see and manage which external clients (MCP apps, third-party REST integrations) have access to which
 * of their buckets. First-party auth only — rejects external-audience tokens via the {@code auth.getScope() != null}
 * check used elsewhere in the codebase.
 */
public class ConnectedAppsController extends ControllerSupport {

	private static final int LIMIT = 100;

	private final CommandDispatcher dispatcher;
	private final ExternalClientRepository clients;
	private final ExternalBucketGrantRepository grants;
	private final UserRepository users;

	@Inject
	public ConnectedAppsController(
		AuthorizationContext auth,
		CommandDispatcher dispatcher,
		ExternalClientRepository clients,
		ExternalBucketGrantRepository grants,
		UserRepository users
	) {
		super(auth);
		this.dispatcher = dispatcher;
		this.clients = clients;
		this.grants = grants;
		this.users = users;
	}

	/** {@code GET /users/{userId}/connected-apps/} — list connected clients with their granted bucket IDs. */
	public void list(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		Identity principal = check(auth, req, res);
		if (principal == null) {
			return;
		}
		PartialList<ExternalClient> connected = clients.find(
			new ExternalClientQuery().userEqualTo(principal),
			0,
			LIMIT
		);
		ObjectNode result = Nodes.newObject();
		PartialList.TOTAL.setValue(result, (int) connected.getTotal());
		ArrayNode array = result.putArray("connected_apps");
		for (ExternalClient client : connected) {
			array.add(toJson(client, grants.grantedBuckets(principal, client.getClient())));
		}
		sendOk(res, result);
	}

	/**
	 * {@code PUT /users/{userId}/connected-apps/{clientId}/grants} — replace the grant set for one client. Body is
	 * {@code {"bucket_ids": ["..."], "rights": "read"}}. Diffs against the existing set and dispatches a
	 * {@link CreateExternalBucketGrantCommand} or {@link DeleteExternalBucketGrantCommand} per change.
	 */
	public void putGrants(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		Identity principal = check(auth, req, res);
		if (principal == null || auth == null) {
			return;
		}
		Identity client = new Identity(req.path().pathParameters().get("clientId"));
		ObjectNode body = body(req);
		String rights = textOr(body, "rights", ExternalBucketGrant.RIGHT_READ);
		if (!ExternalBucketGrant.RIGHT_READ.equals(rights)) {
			sendBadRequest(res, "Only 'read' rights are supported");
			return;
		}
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		Set<String> desired = parseBucketIds(body);
		ImmutableSet<String> existing = grants.grantedBuckets(principal, client);

		for (String bucketId : desired) {
			if (!existing.contains(bucketId)) {
				ExternalBucketGrant grant = new ExternalBucketGrant(principal, client, bucketId, rights);
				dispatcher.dispatch(new CreateExternalBucketGrantCommand(auth.getPrincipal(), grant));
			}
		}
		for (String bucketId : existing) {
			if (!desired.contains(bucketId)) {
				ExternalBucketGrant grant = grants.find(principal, client, bucketId);
				if (grant != null) {
					dispatcher.dispatch(new DeleteExternalBucketGrantCommand(auth.getPrincipal(), grant));
				}
			}
		}
		ExternalClient existingClient = clients.find(principal, client);
		ImmutableSet<String> nowGranted = grants.grantedBuckets(principal, client);
		sendOk(
			res,
			toJson(existingClient != null ? existingClient : new ExternalClient(principal, client), nowGranted)
		);
	}

	/** {@code DELETE /users/{userId}/connected-apps/{clientId}} — revoke every grant for this client. */
	public void revoke(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		Identity principal = check(auth, req, res);
		if (principal == null || auth == null) {
			return;
		}
		Identity client = new Identity(req.path().pathParameters().get("clientId"));
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		for (String bucketId : grants.grantedBuckets(principal, client)) {
			ExternalBucketGrant grant = grants.find(principal, client, bucketId);
			if (grant != null) {
				dispatcher.dispatch(new DeleteExternalBucketGrantCommand(auth.getPrincipal(), grant));
			}
		}
		clients.delete(principal, client);
		sendNoContent(res);
	}

	/** First-party auth + self-or-superuser path check, mirroring {@code BucketListController.findByUser}. */
	private @Nullable Identity check(@Nullable Authorization auth, ServerRequest req, ServerResponse res) {
		if (auth == null) {
			sendUnauthorized(res);
			return null;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return null;
		}
		String userId = req.path().pathParameters().get("userId");
		Identity principal = new UserLookup(users).getIdentity(userId);
		if (principal == null) {
			sendNotFound(res, "user not found");
			return null;
		}
		if (!auth.getPrincipal().equals(principal) && !users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return null;
		}
		return principal;
	}

	private static ObjectNode toJson(ExternalClient client, Set<String> bucketIds) {
		ObjectNode node = Nodes.newObject();
		node.put("client_id", client.getClient().id());
		if (client.getName() != null) {
			node.put("client_name", client.getName());
		}
		node.put("first_seen_at", client.getFirstSeen().toString());
		node.put("last_used_at", client.getLastUsed().toString());
		ArrayNode array = node.putArray("granted_bucket_ids");
		for (String bucketId : bucketIds) {
			array.add(bucketId);
		}
		return node;
	}

	private static Set<String> parseBucketIds(ObjectNode body) {
		Set<String> result = new HashSet<>();
		JsonNode array = body.get("bucket_ids");
		if (array != null && array.isArray()) {
			for (JsonNode value : array) {
				if (value.isTextual() && !value.asText().isBlank()) {
					result.add(value.asText());
				}
			}
		}
		return result;
	}

	private static String textOr(ObjectNode body, String key, String defaultValue) {
		JsonNode value = body.get(key);
		return value != null && value.isTextual() ? value.asText() : defaultValue;
	}
}
