package com.zenobase.controllers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.IdentityProvider;
import com.zenobase.commands.UpdateExternalClientGrantsCommand;
import com.zenobase.common.PartialList;
import com.zenobase.json.Nodes;
import com.zenobase.models.Bucket;
import com.zenobase.models.ExternalClient;
import com.zenobase.models.Identity;
import com.zenobase.models.Role;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.repositories.BucketRepository;
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
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Lets a user see and manage which external clients (MCP apps, third-party REST integrations) have access to which
 * of their buckets. First-party auth only — rejects external-audience tokens via the {@code auth.getScope() != null}
 * check used elsewhere in the codebase.
 */
public class ExternalClientController extends ControllerSupport {

	private static final int LIMIT = 100;

	private final CommandDispatcher dispatcher;
	private final ExternalClientRepository clients;
	private final BucketRepository buckets;
	private final UserRepository users;
	private final IdentityProvider identityProvider;

	@Inject
	public ExternalClientController(
		AuthorizationContext auth,
		CommandDispatcher dispatcher,
		ExternalClientRepository clients,
		BucketRepository buckets,
		UserRepository users,
		IdentityProvider identityProvider
	) {
		super(auth);
		this.dispatcher = dispatcher;
		this.clients = clients;
		this.buckets = buckets;
		this.users = users;
		this.identityProvider = identityProvider;
	}

	/**
	 * {@code GET /external-clients/} — superuser-only paginated view across all users. Each row includes a
	 * {@code principal} field so the admin can see which user owns the client.
	 */
	public void listAll(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		if (auth.getScope() != null) {
			sendForbidden(res);
			return;
		}
		if (!users.isSuperuser(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		int offset = Integer.parseInt(req.query().first("offset").orElse("0"));
		int limit = Integer.parseInt(req.query().first("limit").orElse("10"));
		PartialList<ExternalClient> all = clients.find(new ExternalClientQuery(), offset, limit);
		ObjectNode result = Nodes.newObject();
		PartialList.TOTAL.setValue(result, (int) all.getTotal());
		ArrayNode array = result.putArray("external_clients");
		for (ExternalClient client : all) {
			ObjectNode node = toJson(client);
			node.put("principal", client.getUser().id());
			array.add(node);
		}
		sendOk(res, result);
	}

	/** {@code GET /users/{userId}/external-clients/} — list external clients with their readable bucket ids. */
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
		ArrayNode array = result.putArray("external_clients");
		for (ExternalClient client : connected) {
			array.add(toJson(client));
		}
		sendOk(res, result);
	}

	/**
	 * {@code PUT /users/{userId}/external-clients/{clientId}} — replace the grant set for one client. Body:
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
		// Validate that each requested bucket is one the user actually owns. Without this, the user could persist
		// arbitrary bucket ids on their grant record — currently inert because Bucket.hasRole gates reads, but it would
		// pollute the audit trail and become exploitable if the role check ever loosened.
		for (String bucketId : readableBuckets) {
			Bucket bucket = buckets.find(bucketId);
			if (bucket == null || !bucket.hasRole(new Authorization(principal), Role.OWNER)) {
				sendBadRequest(res, "not the owner of bucket: " + bucketId);
				return;
			}
		}
		dispatcher.dispatch(
			new UpdateExternalClientGrantsCommand(auth.getPrincipal(), principal, client, readableBuckets)
		);
		ExternalClient updated = clients.find(principal, client);
		sendOk(
			res,
			toJson(
				updated != null ? updated : new ExternalClient(principal, client, null, DateTime.now(DateTimeZone.UTC))
			)
		);
	}

	/** {@code DELETE /users/{userId}/external-clients/{clientId}} — revoke this client entirely. */
	public void revoke(ServerRequest req, ServerResponse res) {
		Authorization auth = getCurrentAuthorization(req);
		Identity principal = check(auth, req, res);
		if (principal == null || auth == null) {
			return;
		}
		Identity client = new Identity(req.path().pathParameters().get("clientId"));
		// Existence check up front. Without this, an authenticated user could pass any Auth0 client_id in the URL
		// (e.g. the SPA's or the M2M client_id) and trigger {@link IdentityProvider#deleteApplication} below, since
		// the safety query would correctly see zero references for a first-party client_id (it never gets recorded
		// in external_clients) and we'd happily delete a production Auth0 Application. Returning 404 here keeps the
		// behavior in line with {@link #put}.
		if (clients.find(principal, client) == null) {
			sendNotFound(res, "connected app not found");
			return;
		}
		if (sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		// Snapshot to empty (audited) and then delete the row.
		dispatcher.dispatch(new UpdateExternalClientGrantsCommand(auth.getPrincipal(), principal, client, List.of()));
		clients.delete(principal, client);
		// Delete the corresponding Auth0 Application — but only if no other user still has a row pointing at the same
		// client_id. MCP DCR usually mints a fresh app per installation so this is almost always the last reference,
		// but the (user, client) composite key permits sharing, so we guard against breaking another user's integration.
		// Best-effort: a failed Auth0 call must not fail the revoke (matches the deleteUser pattern).
		if (clients.find(new ExternalClientQuery().clientEqualTo(client), 0, 1).getTotal() == 0) {
			identityProvider.deleteApplication(client);
		}
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
		node.put("created", client.getCreated().toString());
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
