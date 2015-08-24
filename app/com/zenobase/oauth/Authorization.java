package com.zenobase.oauth;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Generator;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class Authorization extends DomainNode {

	public static final String TYPE_NAME = "authorization";

	public static final TokenField ID = new TokenField("@id");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final IdentityField CLIENT = new IdentityField("client");
	public static final TokenField SCOPE = new TokenField("scope");

	public Authorization(ObjectNode node) {
		super(node);
	}

	public Authorization(Identity principal) {
		this(principal, null, null);
	}

	public Authorization(Identity principal, Identity client, String scope) {
		this(principal, client, scope, new DateTime(DateTimeZone.UTC));
	}

	public Authorization(Identity principal, Identity client, String scope, DateTime created) {
		setValue(ID, Generator.longId());
		setValue(PRINCIPAL, principal);
		setValue(CLIENT, client);
		setValue(SCOPE, scope);
		setValue(CREATED, created);
	}

	@Override
	public String getId() {
		return getValue(ID);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public Identity getPrincipal() {
		return getValue(PRINCIPAL);
	}

	public Identity getClient() {
		return getValue(CLIENT);
	}

	public String getScope() {
		return getValue(SCOPE);
	}

	public boolean isPermitted(Authorization auth) {
		return auth.getScope() == null
			&& getPrincipal().equals(auth.getPrincipal());
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(VERSION).add(ID).add(CREATED)
			.add(PRINCIPAL).add(CLIENT).add(SCOPE).build();
	}

	public Authorization copy() {
		return new Authorization(toJson().deepCopy());
	}
}
