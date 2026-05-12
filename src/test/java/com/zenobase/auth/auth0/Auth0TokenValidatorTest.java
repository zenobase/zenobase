package com.zenobase.auth.auth0;

import static org.assertj.core.api.Assertions.assertThat;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.sun.net.httpserver.HttpServer;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.time.Instant;
import java.util.Base64;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

public class Auth0TokenValidatorTest {

	private static final String KID = "test-key-1";
	private static final String AUDIENCE = "https://api.zenobase.com";
	private static final String EXTERNAL_AUDIENCE = "https://api.zenobase.com/external";

	private static HttpServer jwksServer;
	private static int jwksPort;
	private static RSAPublicKey publicKey;
	private static RSAPrivateKey privateKey;
	private static Auth0TokenValidator validator;

	@BeforeAll
	public static void setUp() throws Exception {
		KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA");
		keyGen.initialize(2048);
		KeyPair keyPair = keyGen.generateKeyPair();
		publicKey = (RSAPublicKey) keyPair.getPublic();
		privateKey = (RSAPrivateKey) keyPair.getPrivate();

		String jwksJson = buildJwksJson(publicKey, KID);
		jwksServer = HttpServer.create(new java.net.InetSocketAddress(0), 0);
		jwksPort = jwksServer.getAddress().getPort();
		jwksServer.createContext("/.well-known/jwks.json", exchange -> {
			byte[] response = jwksJson.getBytes();
			exchange.getResponseHeaders().set("Content-Type", "application/json");
			exchange.sendResponseHeaders(200, response.length);
			exchange.getResponseBody().write(response);
			exchange.getResponseBody().close();
		});
		jwksServer.start();

		String domain = "http://localhost:" + jwksPort;
		validator = new Auth0TokenValidator(domain, AUDIENCE, EXTERNAL_AUDIENCE, "");
	}

	@AfterAll
	public static void tearDown() {
		if (jwksServer != null) {
			jwksServer.stop(0);
		}
	}

	@Test
	public void testValidToken() {
		String token = createToken("auth0|user-123", "user@example.com", true);

		Auth0TokenValidator.Auth0Claims claims = validator.validate(token);

		assertThat(claims).isNotNull();
		assertThat(claims.externalId()).isEqualTo("auth0|user-123");
		assertThat(claims.email()).isEqualTo("user@example.com");
		assertThat(claims.emailVerified()).isTrue();
		assertThat(claims.audience()).isEqualTo(AUDIENCE);
	}

	@Test
	public void testValidTokenForExternalAudience() {
		String token = JWT.create()
			.withKeyId(KID)
			.withIssuer("http://localhost:" + jwksPort + "/")
			.withAudience(EXTERNAL_AUDIENCE)
			.withSubject("auth0|user-123")
			.withClaim(Auth0TokenValidator.AZP_CLAIM, "client-xyz")
			.withIssuedAt(Instant.now())
			.withExpiresAt(Instant.now().plusSeconds(3600))
			.sign(Algorithm.RSA256(publicKey, privateKey));

		Auth0TokenValidator.Auth0Claims claims = validator.validate(token);

		assertThat(claims).isNotNull();
		assertThat(claims.externalId()).isEqualTo("auth0|user-123");
		assertThat(claims.audience()).isEqualTo(EXTERNAL_AUDIENCE);
		assertThat(claims.clientId()).isEqualTo("client-xyz");
	}

	@Test
	public void testRejectsExpiredToken() {
		String token = JWT.create()
			.withKeyId(KID)
			.withIssuer("http://localhost:" + jwksPort + "/")
			.withAudience(AUDIENCE)
			.withSubject("auth0|user")
			.withIssuedAt(Instant.now().minusSeconds(7200))
			.withExpiresAt(Instant.now().minusSeconds(3600))
			.sign(Algorithm.RSA256(publicKey, privateKey));

		assertThat(validator.validate(token)).isNull();
	}

	@Test
	public void testRejectsWrongAudience() {
		String token = JWT.create()
			.withKeyId(KID)
			.withIssuer("http://localhost:" + jwksPort + "/")
			.withAudience("https://wrong-audience.com")
			.withSubject("auth0|user")
			.withIssuedAt(Instant.now())
			.withExpiresAt(Instant.now().plusSeconds(3600))
			.sign(Algorithm.RSA256(publicKey, privateKey));

		assertThat(validator.validate(token)).isNull();
	}

	@Test
	public void testRejectsWrongIssuer() {
		String token = JWT.create()
			.withKeyId(KID)
			.withIssuer("https://wrong-issuer.com/")
			.withAudience(AUDIENCE)
			.withSubject("auth0|user")
			.withIssuedAt(Instant.now())
			.withExpiresAt(Instant.now().plusSeconds(3600))
			.sign(Algorithm.RSA256(publicKey, privateKey));

		assertThat(validator.validate(token)).isNull();
	}

	@Test
	public void testRejectsTokenWithNoSub() {
		String token = JWT.create()
			.withKeyId(KID)
			.withIssuer("http://localhost:" + jwksPort + "/")
			.withAudience(AUDIENCE)
			.withIssuedAt(Instant.now())
			.withExpiresAt(Instant.now().plusSeconds(3600))
			.sign(Algorithm.RSA256(publicKey, privateKey));

		assertThat(validator.validate(token)).isNull();
	}

	@Test
	public void testRejectsGarbageToken() {
		assertThat(validator.validate("not.a.jwt")).isNull();
	}

	private String createToken(String subject, String email, boolean emailVerified) {
		return JWT.create()
			.withKeyId(KID)
			.withIssuer("http://localhost:" + jwksPort + "/")
			.withAudience(AUDIENCE)
			.withSubject(subject)
			.withClaim(Auth0TokenValidator.EMAIL_CLAIM, email)
			.withClaim(Auth0TokenValidator.EMAIL_VERIFIED_CLAIM, emailVerified)
			.withIssuedAt(Instant.now())
			.withExpiresAt(Instant.now().plusSeconds(3600))
			.sign(Algorithm.RSA256(publicKey, privateKey));
	}

	private static String buildJwksJson(RSAPublicKey key, String kid) {
		String n = Base64.getUrlEncoder().withoutPadding().encodeToString(toUnsignedBytes(key.getModulus()));
		String e = Base64.getUrlEncoder().withoutPadding().encodeToString(toUnsignedBytes(key.getPublicExponent()));
		return (
			"{\"keys\":[{\"kty\":\"RSA\",\"kid\":\"" +
			kid +
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
