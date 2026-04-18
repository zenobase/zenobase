package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.common.Generator;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.Field;
import com.zenobase.json.IdentityField;
import com.zenobase.json.ObjectField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;
import com.zenobase.oauth.Authorization;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

public class Credentials extends DomainNode {

	public static final String TYPE_NAME = "credentials";

	public static final TokenField ID = new TokenField("@id");
	public static final TokenField TYPE = new TokenField("type");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField AUTHORIZATION_URL = new TokenField("authorizationUrl");
	public static final ObjectField CREDENTIALS = new ObjectField("credentials");

	public Credentials(ObjectNode node) {
		super(node);
	}

	public Credentials(String type, Identity principal) {
		this(type, principal, DateTime.now(DateTimeZone.UTC));
	}

	public Credentials(String type, Identity principal, DateTime created) {
		setValue(ID, Generator.id());
		setValue(TYPE, type);
		setValue(PRINCIPAL, principal);
		setValue(CREATED, created);
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public String getType() {
		return Objects.requireNonNull(getValue(TYPE));
	}

	public Identity getPrincipal() {
		return Objects.requireNonNull(getValue(PRINCIPAL));
	}

	public DateTime getCreated() {
		return Objects.requireNonNull(getValue(CREATED));
	}

	public boolean isAuthorized() {
		return getValue(AUTHORIZATION_URL) == null;
	}

	public @Nullable String getAuthorizationUrl() {
		return getValue(AUTHORIZATION_URL);
	}

	public void setAuthorizationUrl(String authorizationUrl) {
		setValue(AUTHORIZATION_URL, authorizationUrl);
	}

	protected <T> @Nullable T getCredential(Field<T> field) {
		return getValue(CREDENTIALS, field);
	}

	protected <T> void setCredential(Field<T> field, T value) {
		setValue(CREDENTIALS, field, value);
	}

	public boolean isPermitted(Authorization auth) {
		return auth.getScope() == null && getPrincipal().equals(auth.getPrincipal());
	}

	public Credentials copy() {
		return copy(getClass());
	}

	/**
	 * Create a copy of this task with sensitive fields cleared.
	 */
	public Credentials sanitized() {
		Credentials sanitized = copy();
		sanitized.setValue(CREDENTIALS, null);
		return sanitized;
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(VERSION)
			.add(ID)
			.add(TYPE)
			.add(PRINCIPAL)
			.add(CREATED)
			.add(AUTHORIZATION_URL)
			.add(CREDENTIALS)
			.build();
	}
}
