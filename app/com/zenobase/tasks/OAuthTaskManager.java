package com.zenobase.tasks;

import java.io.IOException;
import java.io.InputStreamReader;
import java.util.zip.GZIPInputStream;

import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;
import play.mvc.Http;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;

import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;

public abstract class OAuthTaskManager extends TaskManager {

	private final OAuthCredentialsManager credentialsManager;

	protected OAuthTaskManager(String type, OAuthCredentialsManager credentialsManager) {
		super(type);
		this.credentialsManager = credentialsManager;
	}

	@Override
	public abstract Task newTask(String bucketId, Identity principal, ObjectNode settings);

	@Override
	public Command execute(Task task) {
		try {
			OAuthCredentials credentials = getCredentials(task.getPrincipal());
			return credentials != null ? execute(task, credentials) : null;
		} catch (InvalidTokenException e) {
			return createCommand(e);
		}
	}

	private OAuthCredentials getCredentials(Identity principal) {
		return check(credentialsManager.find(principal));
	}

	private OAuthCredentials check(Credentials credentials) {
		if (credentials == null) {
			throw new MissingCredentialsException(credentialsManager.getType());
		}
		return check(credentials.as(OAuthCredentials.class));
	}

	private OAuthCredentials check(OAuthCredentials credentials) {
		if (!credentials.isAuthorized()) {
			throw new IncompleteCredentialsException(credentials.as(OAuthCredentials.class));
		}
		return credentials.as(OAuthCredentials.class);
	}

	public abstract Command execute(Task task, OAuthCredentials credentials);

	protected void reauthorize(OAuthCredentials credentials) {
		credentialsManager.reauthorize(credentials);
	}

	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		Response response = credentialsManager.send(request, credentials);
		if (!response.isSuccessful()) {
			if (response.getCode() == Http.Status.UNAUTHORIZED) {
				throw new InvalidTokenException(request, credentials);
			} else {
				throw new InvalidStatusException(request, response.getCode());
			}
		}
		return response;
	}

	protected static JsonNode parse(Response response) {
		return Nodes.read(getBody(response));
	}

	protected static ObjectNode parseObject(Response response) {
		return Nodes.readObject(getBody(response));
	}

	protected static ArrayNode parseArray(Response response) {
		return Nodes.readArray(getBody(response));
	}

	private static String getBody(Response response) {
		if ("gzip".equals(response.getHeader("Content-Encoding"))) {
			InputStreamReader in = null;
			try {
				in = new InputStreamReader(new GZIPInputStream(response.getStream()), Charsets.UTF_8);
				return CharStreams.toString(in);
			} catch (IOException e) {
				throw new RuntimeException(e);
			} finally {
				if (in != null) {
					try {
						in.close();
					} catch (IOException e) {

					}
				}
			}
		}
		return response.getBody();
	}

	protected Command createCommand(InvalidTokenException e) {
		Token requestToken = credentialsManager.getRequestToken(e.getCredentials());
		return UpdateCredentialsCommand.builder(e.getCredentials())
			.set(Credentials.AUTHORIZATION_URL, e.getCredentials().getAuthorizationUrl(), credentialsManager.getService(e.getCredentials()).getAuthorizationUrl(requestToken))
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, e.getCredentials().getToken(), requestToken)
			.build();
	}
}
