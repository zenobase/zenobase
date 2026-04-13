package com.zenobase.auth.auth0;

import java.math.BigInteger;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.KeyFactory;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.RSAPublicKeySpec;
import java.time.Duration;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.models.Identity;

public class Auth0TokenValidator {

	private static final Logger logger = LoggerFactory.getLogger(Auth0TokenValidator.class);
	private static final Duration JWKS_TIMEOUT = Duration.ofSeconds(10);

	static final String ZENOBASE_ID_CLAIM = "https://zenobase.com/zenobase_id";
	static final String USERNAME_CLAIM = "https://zenobase.com/username";
	static final String EMAIL_CLAIM = "https://zenobase.com/email";
	static final String EMAIL_VERIFIED_CLAIM = "https://zenobase.com/email_verified";

	public record Auth0Claims(
			Identity identity,
			@Nullable String username,
			@Nullable String email,
			boolean emailVerified,
			@Nullable String externalId) {}

	private final String issuer;
	private final JWTVerifier verifier;

	@Inject
	public Auth0TokenValidator(
			@Named("auth0.domain") String domain,
			@Named("auth0.audience") String audience,
			@Named("auth0.jwks_domain") String jwksDomain) {
		String jwksUrl = (jwksDomain.isEmpty() ? domain : jwksDomain) + "/.well-known/jwks.json";
		HttpClient httpClient =
				HttpClient.newBuilder().connectTimeout(JWKS_TIMEOUT).build();
		Map<String, RSAPublicKey> keyCache = new ConcurrentHashMap<>();
		RSAKeyProvider keyProvider = new RSAKeyProvider() {
			@Override
			public RSAPublicKey getPublicKeyById(String keyId) {
				RSAPublicKey cached = keyCache.get(keyId);
				if (cached != null) {
					return cached;
				}
				Map<String, RSAPublicKey> fresh;
				try {
					fresh = fetchKeys(httpClient, jwksUrl);
				} catch (Exception e) {
					throw new RuntimeException("Failed to fetch JWKS from " + jwksUrl, e);
				}
				keyCache.putAll(fresh);
				cached = keyCache.get(keyId);
				if (cached == null) {
					throw new RuntimeException("Key " + keyId + " not found in JWKS at " + jwksUrl);
				}
				return cached;
			}

			@Override
			public java.security.interfaces.@Nullable RSAPrivateKey getPrivateKey() {
				return null;
			}

			@Override
			public @Nullable String getPrivateKeyId() {
				return null;
			}
		};
		Algorithm algorithm = Algorithm.RSA256(keyProvider);
		this.issuer = domain.startsWith("http") ? domain + "/" : "https://" + domain + "/";
		this.verifier =
				JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build();
	}

	public String issuer() {
		return issuer;
	}

	public @Nullable Auth0Claims validate(String token) {
		try {
			DecodedJWT jwt = verifier.verify(token);
			String zenobaseId = jwt.getClaim(ZENOBASE_ID_CLAIM).asString();
			if (zenobaseId == null) {
				// Fall back to sub claim (e.g. for localauth0 in dev)
				zenobaseId = jwt.getSubject();
			}
			if (zenobaseId == null) {
				logger.warn("Auth0 JWT missing both {} and sub claims", ZENOBASE_ID_CLAIM);
				return null;
			}
			String username = jwt.getClaim(USERNAME_CLAIM).asString();
			String email = jwt.getClaim(EMAIL_CLAIM).asString();
			boolean emailVerified = parseBoolean(jwt.getClaim(EMAIL_VERIFIED_CLAIM));
			return new Auth0Claims(new Identity(zenobaseId), username, email, emailVerified, jwt.getSubject());
		} catch (Exception e) {
			logger.debug("Auth0 JWT validation failed: {}", e.getMessage());
			return null;
		}
	}

	private static boolean parseBoolean(com.auth0.jwt.interfaces.Claim claim) {
		if (claim.isMissing() || claim.isNull()) {
			return false;
		}
		Boolean b = claim.asBoolean();
		if (b != null) {
			return b;
		}
		return "true".equals(claim.asString());
	}

	private static Map<String, RSAPublicKey> fetchKeys(HttpClient client, String jwksUrl) throws Exception {
		HttpRequest request = HttpRequest.newBuilder()
				.uri(URI.create(jwksUrl))
				.timeout(JWKS_TIMEOUT)
				.GET()
				.build();
		HttpResponse<String> response = client.send(request, HttpResponse.BodyHandlers.ofString());
		if (response.statusCode() != 200) {
			throw new RuntimeException("JWKS fetch returned " + response.statusCode());
		}
		ObjectMapper mapper = new ObjectMapper();
		JsonNode root = mapper.readTree(response.body());
		JsonNode keys = root.get("keys");
		if (keys == null || !keys.isArray()) {
			throw new RuntimeException("Invalid JWKS response: no keys array");
		}
		Map<String, RSAPublicKey> result = new HashMap<>();
		for (JsonNode key : keys) {
			String kid = key.get("kid").asText();
			String n = key.get("n").asText();
			String e = key.get("e").asText();
			result.put(kid, buildRSAPublicKey(n, e));
		}
		return result;
	}

	private static RSAPublicKey buildRSAPublicKey(String modulusBase64, String exponentBase64) throws Exception {
		byte[] modulusBytes = Base64.getUrlDecoder().decode(modulusBase64);
		byte[] exponentBytes = Base64.getUrlDecoder().decode(exponentBase64);
		BigInteger modulus = new BigInteger(1, modulusBytes);
		BigInteger exponent = new BigInteger(1, exponentBytes);
		RSAPublicKeySpec spec = new RSAPublicKeySpec(modulus, exponent);
		KeyFactory factory = KeyFactory.getInstance("RSA");
		return (RSAPublicKey) factory.generatePublic(spec);
	}
}
