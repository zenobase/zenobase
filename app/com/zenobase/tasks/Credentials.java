package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

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

public class Credentials extends DomainNode {

	public static final String TYPE_NAME = "credentials";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("type");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField AUTHORIZATION_URL = new TokenField("authorizationUrl");
	public static final ObjectField CREDENTIALS = new ObjectField("credentials");

	public Credentials(ObjectNode node) {
		super(node);
	}

	public Credentials(String type, Identity principal) {
		this(type, principal, new DateTime(DateTimeZone.UTC));
	}

	public Credentials(String type, Identity principal, DateTime created) {
		setValue(ID, Generator.id());
		setValue(TYPE, type);
		setValue(PRINCIPAL, principal);
		setValue(CREATED, created);
	}

	@Override
	public String getId() {
		return getValue(ID);
	}

	public String getType() {
		return getValue(TYPE);
	}

	public Identity getPrincipal() {
		return getValue(PRINCIPAL);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public boolean isAuthorized() {
		return getValue(AUTHORIZATION_URL) == null;
	}

	public String getAuthorizationUrl() {
		return getValue(AUTHORIZATION_URL);
	}

	public void setAuthorizationUrl(String authorizationUrl) {
		setValue(AUTHORIZATION_URL, authorizationUrl);
	}

	protected <T> T getCredential(Field<T> field) {
		return getValue(CREDENTIALS, field);
	}

	protected <T> void setCredential(Field<T> field, T value) {
		setValue(CREDENTIALS, field, value);
	}

	public boolean isPermitted(Authorization auth) {
		return auth.getScope() == null
			&& getPrincipal().equals(auth.getPrincipal());
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
