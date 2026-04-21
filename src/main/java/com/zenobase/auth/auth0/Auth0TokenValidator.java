package com.zenobase.auth.auth0;

import com.auth0.jwk.JwkException;
import com.auth0.jwk.JwkProvider;
import com.auth0.jwk.JwkProviderBuilder;
import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.auth0.jwt.interfaces.RSAKeyProvider;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import java.net.URI;
import java.security.interfaces.RSAPublicKey;
import java.util.concurrent.TimeUnit;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Auth0TokenValidator {

	private static final Logger logger = LoggerFactory.getLogger(Auth0TokenValidator.class);

	static final String USERNAME_CLAIM = "https://zenobase.com/username";
	static final String EMAIL_CLAIM = "https://zenobase.com/email";
	static final String EMAIL_VERIFIED_CLAIM = "https://zenobase.com/email_verified";
	static final String SID_CLAIM = "sid";

	public record Auth0Claims(
		String externalId,
		@Nullable String username,
		@Nullable String email,
		boolean emailVerified,
		@Nullable String sessionId
	) {}

	private final String issuer;
	private final JWTVerifier verifier;

	@Inject
	public Auth0TokenValidator(
		@Named("auth0.domain") String domain,
		@Named("auth0.audience") String audience,
		@Named("auth0.jwks_domain") String jwksDomain
	) {
		String jwksUrl = (jwksDomain.isEmpty() ? domain : jwksDomain) + "/.well-known/jwks.json";
		JwkProvider jwkProvider;
		try {
			jwkProvider = new JwkProviderBuilder(URI.create(jwksUrl).toURL())
				.cached(5, 1, TimeUnit.HOURS)
				.rateLimited(10, 1, TimeUnit.MINUTES)
				.build();
		} catch (Exception e) {
			throw new RuntimeException("Invalid JWKS URL: " + jwksUrl, e);
		}
		RSAKeyProvider keyProvider = new RSAKeyProvider() {
			@Override
			public RSAPublicKey getPublicKeyById(String keyId) {
				try {
					return (RSAPublicKey) jwkProvider.get(keyId).getPublicKey();
				} catch (JwkException e) {
					throw new RuntimeException("JWKS lookup failed for key " + keyId, e);
				}
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
		this.verifier = JWT.require(algorithm).withIssuer(issuer).withAudience(audience).build();
	}

	public String issuer() {
		return issuer;
	}

	public @Nullable Auth0Claims validate(String token) {
		try {
			DecodedJWT jwt = verifier.verify(token);
			String subject = jwt.getSubject();
			if (subject == null) {
				logger.warn("Auth0 JWT missing subject claim");
				return null;
			}
			String username = jwt.getClaim(USERNAME_CLAIM).asString();
			String email = jwt.getClaim(EMAIL_CLAIM).asString();
			boolean emailVerified = parseBoolean(jwt.getClaim(EMAIL_VERIFIED_CLAIM));
			String sessionId = jwt.getClaim(SID_CLAIM).asString();
			return new Auth0Claims(subject, username, email, emailVerified, sessionId);
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
}
