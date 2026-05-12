package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.UpdateExternalClientGrantsCommand;
import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
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
	private final UserRepository users;

	@Inject
	public ConnectedAppsController(
		AuthorizationContext auth,
		CommandDispatcher dispatcher,
		ExternalClientRepository clients,
		UserRepository users
	) {
		super(auth);
		this.dispatcher = dispatcher;
		this.clients = clients;
		this.users = users;
	}

	/** {@code GET /users/{userId}/connected-apps/} — list connected clients with their readable bucket ids. */
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
			array.add(toJson(client));
		}
		sendOk(res, result);
	}

	/**
	 * {@code PUT /users/{userId}/connected-apps/{clientId}} — replace the grant set for one client. Body:
	 * {@code {"readable_buckets": [...]}}. Dispatches a single {@link UpdateExternalClientGrantsCommand} that snapshots
	 * the new set.
	 */
	public void put(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		Identity principal = check(auth, req, res);
		if (principal == null || auth == null) {
			return;
		}
		Identity client = new Identity(req.path().pathParameters().get("clientId"));
		if (clients.find(principal, client) == null) {
			sendNotFound(res, "connected app not found");
			return;
		}
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		ObjectNode body = body(req);
		List<String> readableBuckets = parseBucketIds(body, "readable_buckets");
		dispatcher.dispatch(
			new UpdateExternalClientGrantsCommand(auth.getPrincipal(), principal, client, readableBuckets)
		);
		ExternalClient updated = clients.find(principal, client);
		sendOk(
			res,
			toJson(
				updated != null
					? updated
					: new ExternalClient(
							principal,
							client,
							null,
							org.joda.time.DateTime.now(org.joda.time.DateTimeZone.UTC)
						)
			)
		);
	}

	/** {@code DELETE /users/{userId}/connected-apps/{clientId}} — revoke this client entirely. */
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
		// Snapshot to empty (audited) and then delete the row.
		dispatcher.dispatch(new UpdateExternalClientGrantsCommand(auth.getPrincipal(), principal, client, List.of()));
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

	private static ObjectNode toJson(ExternalClient client) {
		ObjectNode node = Nodes.newObject();
		node.put("client_id", client.getClient().id());
		if (client.getName() != null) {
			node.put("client_name", client.getName());
		}
		node.put("first_seen_at", client.getFirstSeen().toString());
		ArrayNode readable = node.putArray("readable_buckets");
		for (String bucketId : client.getReadableBuckets()) {
			readable.add(bucketId);
		}
		return node;
	}

	private static List<String> parseBucketIds(ObjectNode body, String field) {
		// LinkedHashSet preserves the order from the body while deduping
		Set<String> result = new LinkedHashSet<>();
		JsonNode array = body.get(field);
		if (array != null && array.isArray()) {
			for (JsonNode value : array) {
				if (value.isTextual() && !value.asText().isBlank()) {
					result.add(value.asText());
				}
			}
		}
		return new ArrayList<>(result);
	}
}
