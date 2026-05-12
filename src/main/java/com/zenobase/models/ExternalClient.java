package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IdentityField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;

/**
 * Per-({@link Identity user}, {@code client}) display metadata for external clients (MCP, third-party REST). One row
 * per (user, client_id) pair. Written opportunistically on token observation — not command-audited because it's purely
 * informational and recreatable. Used by the Connected Apps UI to show "this client first connected on...".
 */
public class ExternalClient extends DomainNode {

	public static final String TYPE_NAME = "external_client";

	public static final TokenField ID = new TokenField("@id");
	public static final IdentityField USER = new IdentityField("user");
	public static final IdentityField CLIENT = new IdentityField("client");
	public static final TextField NAME = new TextField("name");
	public static final DateTimeField FIRST_SEEN = new DateTimeField("first_seen");
	public static final DateTimeField LAST_USED = new DateTimeField("last_used");

	public static final Schema SCHEMA = new SchemaBuilder(TYPE_NAME)
		.add(VERSION)
		.add(ID)
		.add(USER)
		.add(CLIENT)
		.add(NAME)
		.add(FIRST_SEEN)
		.add(LAST_USED)
		.build();

	public ExternalClient(ObjectNode node) {
		super(node);
	}

	public ExternalClient(Identity user, Identity client) {
		DateTime now = DateTime.now(DateTimeZone.UTC);
		setValue(ID, id(user, client));
		setValue(USER, user);
		setValue(CLIENT, client);
		setValue(FIRST_SEEN, now);
		setValue(LAST_USED, now);
	}

	public static String id(Identity user, Identity client) {
		return user.id() + "|" + client.id();
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public Identity getUser() {
		return Objects.requireNonNull(getValue(USER));
	}

	public Identity getClient() {
		return Objects.requireNonNull(getValue(CLIENT));
	}

	public @Nullable String getName() {
		return getValue(NAME);
	}

	public void setName(@Nullable String name) {
		setValue(NAME, name);
	}

	public DateTime getFirstSeen() {
		return Objects.requireNonNull(getValue(FIRST_SEEN));
	}

	public DateTime getLastUsed() {
		return Objects.requireNonNull(getValue(LAST_USED));
	}

	public void touch() {
		setValue(LAST_USED, DateTime.now(DateTimeZone.UTC));
	}
}
