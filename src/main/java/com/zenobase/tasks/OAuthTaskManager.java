package com.zenobase.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.io.CharStreams;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.zip.GZIPInputStream;
import org.jspecify.annotations.Nullable;
import org.scribe.model.OAuthRequest;
import org.scribe.model.Response;
import org.scribe.model.Token;

public abstract class OAuthTaskManager extends TaskManager {

	private final OAuthCredentialsManager credentialsManager;

	protected OAuthTaskManager(String type, OAuthCredentialsManager credentialsManager) {
		super(type);
		this.credentialsManager = credentialsManager;
	}

	@Override
	public abstract Task newTask(String bucketId, Identity principal, ObjectNode settings);

	@Override
	public @Nullable Command execute(Task task) {
		OAuthCredentials credentials = getCredentials(task.getPrincipal());
		return execute(task, credentials);
	}

	private OAuthCredentials getCredentials(Identity principal) {
		return check(credentialsManager.find(principal));
	}

	public Command recoverInvalidToken(InvalidTokenException e) {
		return createCommand(e);
	}

	public OAuthCredentials reload(InvalidTokenException e) {
		return getCredentials(e.getCredentials().getPrincipal());
	}

	private OAuthCredentials check(@Nullable Credentials credentials) {
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

	public abstract @Nullable Command execute(Task task, OAuthCredentials credentials);

	protected void reauthorize(OAuthCredentials credentials) {
		try {
			credentialsManager.reauthorize(credentials);
		} catch (IllegalArgumentException e) {
			throw new InvalidTokenException(credentials);
		}
	}

	protected Response send(OAuthRequest request, OAuthCredentials credentials) {
		request.addHeader("Accept-Encoding", "gzip");
		request.addHeader("User-Agent", "zeno");
		Response response = credentialsManager.send(request, credentials);
		if (!isSuccessful(response)) {
			if (response.getCode() == 401) {
				throw new InvalidTokenException(credentials);
			} else {
				throw new InvalidStatusException(request.getCompleteUrl(), response.getCode(), getBody(response));
			}
		}
		return response;
	}

	protected boolean isSuccessful(Response response) {
		return response.isSuccessful();
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

	protected static String getBody(Response response) {
		if (isEncoded(response, "gzip")) {
			try (
				InputStreamReader in = new InputStreamReader(
					new GZIPInputStream(response.getStream()),
					StandardCharsets.UTF_8
				)
			) {
				return CharStreams.toString(in);
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		return response.getBody();
	}

	private static boolean isEncoded(Response response, String encoding) {
		return (
			encoding.equals(response.getHeader("Content-Encoding")) ||
			encoding.equals(response.getHeader("content-encoding"))
		);
	}

	protected Command createCommand(InvalidTokenException e) {
		Token requestToken = credentialsManager.getRequestToken(e.getCredentials());
		return UpdateCredentialsCommand.builder(e.getCredentials())
			.set(
				Credentials.AUTHORIZATION_URL,
				e.getCredentials().getAuthorizationUrl(),
				credentialsManager.getService(e.getCredentials()).getAuthorizationUrl(requestToken)
			)
			.with(Credentials.CREDENTIALS)
			.set(OAuthCredentials.TOKEN, e.getCredentials().getToken(), requestToken)
			.build();
	}
}
