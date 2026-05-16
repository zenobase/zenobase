package com.zenobase.controllers;

import com.auth0.client.mgmt.core.ManagementApiException;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.auth.IdentityProvider;
import com.zenobase.auth.Passkey;
import com.zenobase.commands.*;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.models.User;
import com.zenobase.oauth.Authorization;
import com.zenobase.queries.BucketQuery;
import com.zenobase.queries.CredentialsQuery;
import com.zenobase.queries.ExternalClientQuery;
import com.zenobase.queries.TaskQuery;
import com.zenobase.repositories.BucketRepository;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.repositories.ExternalClientRepository;
import com.zenobase.repositories.TaskRepository;
import com.zenobase.repositories.UserRepository;
import com.zenobase.services.CommandDispatcher;
import com.zenobase.services.UserLookup;
import io.helidon.webserver.http.ServerRequest;
import io.helidon.webserver.http.ServerResponse;
import jakarta.inject.Inject;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class AccountController extends ControllerSupport {

	private static final Logger logger = LoggerFactory.getLogger(AccountController.class);

	private final UserRepository users;
	private final BucketRepository buckets;
	private final TaskRepository tasks;
	private final CredentialsRepository credentials;
	private final ExternalClientRepository externalClients;
	private final CommandDispatcher dispatcher;
	private final IdentityProvider identityProvider;

	@Inject
	public AccountController(
		AuthorizationContext security,
		UserRepository users,
		BucketRepository buckets,
		TaskRepository tasks,
		CredentialsRepository credentials,
		ExternalClientRepository externalClients,
		CommandDispatcher dispatcher,
		IdentityProvider identityProvider
	) {
		super(security);
		this.users = users;
		this.buckets = buckets;
		this.tasks = tasks;
		this.credentials = credentials;
		this.externalClients = externalClients;
		this.dispatcher = dispatcher;
		this.identityProvider = identityProvider;
	}

	public void close(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res);
			return;
		}
		if (auth.getScope() != null || (!user.is(auth.getPrincipal()) && !users.isSuperuser(auth.getPrincipal()))) {
			sendForbidden(res);
			return;
		}
		if (!user.is(auth.getPrincipal()) && sendForbiddenIfSuspended(auth, res)) {
			return;
		}
		// Snapshot the client_ids this user has connected before dispatch — afterwards the rows are gone and we
		// can't look them up. We need them to issue best-effort Auth0 Application deletes below.
		List<Identity> connectedClientIds = new ArrayList<>();
		externalClients.find(new ExternalClientQuery().userEqualTo(user.asIdentity()), client ->
			connectedClientIds.add(client.getClient())
		);
		Command command = buildCloseAccountCommand(auth.getPrincipal(), user);
		String commandId = dispatcher.dispatch(command);
		identityProvider.deleteUser(user);
		// For each external client this user had, drop the corresponding Auth0 Application — but only if no other
		// user still references the same client_id. Same safety check as ExternalClientController.revoke.
		for (Identity clientId : connectedClientIds) {
			if (externalClients.find(new ExternalClientQuery().clientEqualTo(clientId), 0, 1).getTotal() == 0) {
				identityProvider.deleteApplication(clientId);
			}
		}
		setHeader(res, COMMAND_ID, commandId);
		sendNoContent(res);
	}

	public Command buildCloseAccountCommand(Identity principal, User user) {
		var command = new CompoundCommand(
			principal,
			String.format("closed account %s", user.getName()),
			String.format("reopened account %s", user.getName())
		);
		command.add(new DeleteUserCommand(principal, user));
		buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(true), bucket ->
			command.add(new DeleteBucketCommand(principal, bucket))
		);
		buckets.find(new BucketQuery().principalEqualTo(user.asIdentity()).isAlias(false), bucket ->
			command.add(new DeleteBucketCommand(principal, bucket))
		);
		tasks.find(new TaskQuery().principalEqualTo(user.asIdentity()), task ->
			command.add(new DeleteTaskCommand(principal, task))
		);
		credentials.find(new CredentialsQuery().principalEqualTo(user.asIdentity()), credentials ->
			command.add(new DeleteCredentialsCommand(principal, credentials))
		);
		externalClients.find(new ExternalClientQuery().userEqualTo(user.asIdentity()), externalClient ->
			command.add(new DeleteExternalClientCommand(principal, externalClient))
		);
		return command;
	}

	public void listPasskeys(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res);
			return;
		}
		if (auth.getScope() != null || !user.is(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		ArrayNode passkeys = Nodes.newArray();
		for (Passkey passkey : identityProvider.listPasskeys(user)) {
			passkeys.add(toJson(passkey));
		}
		sendOk(res, passkeys);
	}

	public void deletePasskey(ServerRequest req, ServerResponse res) {
		String userId = req.path().pathParameters().get("userId");
		String passkeyId = req.path().pathParameters().get("passkeyId");
		Authorization auth = getCurrentAuthorization(req);
		if (auth == null) {
			sendUnauthorized(res);
			return;
		}
		User user = new UserLookup(users).getUser(userId);
		if (user == null) {
			sendNotFound(res);
			return;
		}
		if (auth.getScope() != null || !user.is(auth.getPrincipal())) {
			sendForbidden(res);
			return;
		}
		try {
			identityProvider.deletePasskey(user, passkeyId);
		} catch (IllegalArgumentException e) {
			sendNotFound(res);
			return;
		} catch (ManagementApiException e) {
			if (e.statusCode() == 404) {
				sendNotFound(res);
				return;
			}
			logger.error("Passkey delete failed for user {}", user.getId(), e);
			sendInternalServerError(res, "failed to delete passkey");
			return;
		} catch (RuntimeException e) {
			logger.error("Passkey delete failed for user {}", user.getId(), e);
			sendInternalServerError(res, "failed to delete passkey");
			return;
		}
		sendNoContent(res);
	}

	private static ObjectNode toJson(Passkey passkey) {
		ObjectNode node = Nodes.newObject();
		node.put("id", passkey.id());
		if (passkey.name() != null) {
			node.put("name", passkey.name());
		}
		node.put("created", passkey.createdAt());
		if (passkey.lastAuthAt() != null) {
			node.put("last_auth", passkey.lastAuthAt());
		}
		if (passkey.userAgent() != null) {
			node.put("user_agent", passkey.userAgent());
		}
		return node;
	}
}
