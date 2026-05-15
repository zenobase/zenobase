package com.zenobase.mcp;

import com.sun.net.httpserver.HttpServer;
import com.zenobase.auth.auth0.Auth0TokenValidator;
import java.math.BigInteger;
import java.net.InetSocketAddress;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPublicKey;
import java.util.Base64;

/**
 * Shared in-process JWKS server + Auth0TokenValidator factory for MCP-layer tests so each test class doesn't have
 * to repeat the RSA-key plumbing. The JWKS server runs on a random port; one per process.
 */
final class Auth0Fixture {

	private static final String KID = "test-key-1";
	private static final HttpServer SERVER;
	private static final int PORT;
	private static final RSAPublicKey PUBLIC_KEY;

	static {
		try {
			KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
			keyGen.initialize(2048);
			KeyPair keyPair = keyGen.generateKeyPair();
			PUBLIC_KEY = (RSAPublicKey) keyPair.getPublic();

			String jwksJson = jwksJson(PUBLIC_KEY);
			SERVER = HttpServer.create(new InetSocketAddress(0), 0);
			PORT = SERVER.getAddress().getPort();
			SERVER.createContext("/.well-known/jwks.json", exchange -> {
				byte[] response = jwksJson.getBytes();
				exchange.getResponseHeaders().set("Content-Type", "application/json");
				exchange.sendResponseHeaders(200, response.length);
				exchange.getResponseBody().write(response);
				exchange.getResponseBody().close();
			});
			SERVER.start();
			Runtime.getRuntime().addShutdownHook(new Thread(() -> SERVER.stop(0)));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private Auth0Fixture() {}

	static Auth0TokenValidator makeValidator(String internalAudience, String externalAudience) {
		return new Auth0TokenValidator("http://localhost:" + PORT, internalAudience, externalAudience, "");
	}

	private static String jwksJson(RSAPublicKey key) {
		String n = Base64.getUrlEncoder().withoutPadding().encodeToString(toUnsignedBytes(key.getModulus()));
		String e = Base64.getUrlEncoder().withoutPadding().encodeToString(toUnsignedBytes(key.getPublicExponent()));
		return (
			"{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" +
			KID +
			"\",\"use\":\"sig\",\"alg\":\"RS256\",\"n\":\"" +
			n +
			"\",\"e\":\"" +
			e +
			"\"}]}"
		);
	}

	private static byte[] toUnsignedBytes(BigInteger value) {
		byte[] bytes = value.toByteArray();
		if (bytes[0] == 0) {
			byte[] trimmed = new byte[bytes.length - 1];
			System.arraycopy(bytes, 1, trimmed, 0, trimmed.length);
			return trimmed;
		}
		return bytes;
	}
}
