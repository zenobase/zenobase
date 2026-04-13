package com.zenobase.auth.local;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.zenobase.auth.TokenValidator;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;

public class LocalTokenService implements TokenValidator {

	private static final Logger logger = LoggerFactory.getLogger(LocalTokenService.class);

	public static final String ISSUER = "zenobase";
	static final String TYPE_CLAIM = "type";
	static final String CLIENT_CLAIM = "client";
	static final String SCOPE_CLAIM = "scope";
	static final String TYPE_GUEST = "guest";
	static final String TYPE_SCOPED = "scoped";

	private static final long GUEST_TOKEN_DAYS = 31;

	private final Algorithm algorithm;
	private final JWTVerifier verifier;

	@Inject
	public LocalTokenService(@Named("jwt.secret") String secret) {
		this.algorithm = Algorithm.HMAC256(secret);
		this.verifier = JWT.require(algorithm).withIssuer(ISSUER).build();
	}

	public String createGuestToken() {
		Identity guest = new Identity();
		return JWT.create()
				.withIssuer(ISSUER)
				.withSubject(guest.id())
				.withClaim(TYPE_CLAIM, TYPE_GUEST)
				.withIssuedAt(Instant.now())
				.withExpiresAt(Instant.now().plus(GUEST_TOKEN_DAYS, ChronoUnit.DAYS))
				.sign(algorithm);
	}

	public String createScopedToken(Identity principal, Identity client, String scope) {
		return JWT.create()
				.withIssuer(ISSUER)
				.withSubject(principal.id())
				.withClaim(TYPE_CLAIM, TYPE_SCOPED)
				.withClaim(CLIENT_CLAIM, client.id())
				.withClaim(SCOPE_CLAIM, scope)
				.withIssuedAt(Instant.now())
				.sign(algorithm);
	}

	@Override
	public String issuer() {
		return ISSUER;
	}

	@Override
	public @Nullable Authorization validate(String token) {
		try {
			DecodedJWT jwt = verifier.verify(token);
			Identity principal = new Identity(jwt.getSubject());
			String type = jwt.getClaim(TYPE_CLAIM).asString();
			if (TYPE_GUEST.equals(type)) {
				return new Authorization(principal);
			}
			if (TYPE_SCOPED.equals(type)) {
				String clientId = jwt.getClaim(CLIENT_CLAIM).asString();
				String scope = jwt.getClaim(SCOPE_CLAIM).asString();
				Identity client = clientId != null ? new Identity(clientId) : null;
				return new Authorization(principal, client, scope);
			}
			logger.warn("Unknown local JWT type: {}", type);
			return null;
		} catch (Exception e) {
			logger.debug("Local JWT validation failed: {}", e.getMessage());
			return null;
		}
	}
}
