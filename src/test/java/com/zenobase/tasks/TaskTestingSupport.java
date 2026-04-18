package com.zenobase.tasks;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.commands.Command;
import com.zenobase.commands.UpdateCredentialsCommand;
import com.zenobase.common.Generator;
import com.zenobase.json.Nodes;
import com.zenobase.models.Identity;
import com.zenobase.oauth.ExpiringToken;
import com.zenobase.repositories.CredentialsRepository;
import com.zenobase.testing.Manual;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.Scanner;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.net.URLEncodedUtils;
import org.joda.time.DateTime;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.Mockito;
import org.scribe.model.Token;

@Manual
public abstract class TaskTestingSupport {

	protected final Identity principal = new Identity();
	protected final String bucketId = Generator.id();
	protected final String apiKey = System.getProperty("oauth.apiKey");
	protected final String apiSecret = System.getProperty("oauth.apiSecret");
	protected final String callbackUrl = "https://zenobase.com";
	protected final CredentialsRepository repository = Mockito.mock(CredentialsRepository.class);

	@BeforeEach
	public void setUp() {
		Assumptions.assumeTrue(apiKey != null);
		Assumptions.assumeTrue(apiSecret != null);
	}

	protected void run(OAuthTaskManager manager, ObjectNode settings) {
		Task task = manager.newTask(bucketId, principal, settings);
		print(manager.execute(task, getCredentials()).toJson());
	}

	protected void runInApplication(OAuthTaskManager manager, ObjectNode settings) {
		run(manager, settings);
	}

	private OAuthCredentials getCredentials() {
		OAuthCredentials credentials = parseCredentials();
		return credentials != null ? credentials : requestCredentials();
	}

	private static OAuthCredentials parseCredentials() {
		String token = System.getProperty("oauth.token");
		String secret = System.getProperty("oauth.secret", "");
		String refresh = System.getProperty("oauth.refresh");
		String scope = System.getProperty("oauth.scope");
		return token != null ? newCredentials(newToken(token, secret, refresh), scope) : null;
	}

	private static Token newToken(String token, String secret, String refresh) {
		return refresh != null
			? new ExpiringToken(token, secret, DateTime.now().minusMonths(1), refresh)
			: new Token(token, secret);
	}

	private static OAuthCredentials newCredentials(Token token, String scope) {
		OAuthCredentials credentials = new OAuthCredentials(Nodes.newObject());
		credentials.setToken(token);
		credentials.setScope(scope);
		return credentials;
	}

	private OAuthCredentials requestCredentials() {
		OAuthCredentialsManager manager = newCredentialsManager();
		OAuthCredentials credentials = manager.newCredentials(principal);
		System.out.println(credentials.getAuthorizationUrl());
		System.out.print("> ");
		Scanner scanner = new Scanner(System.in);
		ObjectNode config = parseQueryString(scanner.nextLine());
		scanner.close();
		credentials = apply(manager.authorize(credentials, config), credentials).as(OAuthCredentials.class);
		print(credentials);
		return credentials;
	}

	protected abstract OAuthCredentialsManager newCredentialsManager();

	private static ObjectNode parseQueryString(String url) {
		ObjectNode node = Nodes.newObject();
		URI uri = URI.create(url.replace("/#", "").strip());
		for (NameValuePair param : URLEncodedUtils.parse(uri, StandardCharsets.UTF_8)) {
			node.put(param.getName(), param.getValue());
		}
		return node;
	}

	private static Credentials apply(Command command, Credentials credentials) {
		return ((UpdateCredentialsCommand) command).apply(credentials);
	}

	private static void print(OAuthCredentials credentials) {
		print(credentials.getToken());
		if (credentials.getScope() != null) {
			System.out.println("-Doauth.scope=" + credentials.getScope());
		}
	}

	private static void print(Token token) {
		System.out.println("-Doauth.token=" + token.getToken());
		System.out.println("-Doauth.secret=" + token.getSecret());
		if (token instanceof ExpiringToken) {
			System.out.println("-Doauth.refresh=" + ((ExpiringToken) token).getRefreshToken());
		}
	}

	private static void print(JsonNode node) {
		System.out.println(Nodes.toString(node));
	}
}
