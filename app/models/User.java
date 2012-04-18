package models;


import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import schema.BooleanField;
import schema.DateTimeField;
import schema.Schema;
import schema.SchemaBuilder;
import schema.TokenField;

import common.BCrypt;
import common.Nodes;

public class User extends DomainNode {

	public static final String TYPE_NAME = "user";

	public static final TokenField ID = new TokenField("@id");
	public static final TokenField NAME = new TokenField("name", false);
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField PASSWORD = new TokenField("password", false);
	public static final TokenField EMAIL = new TokenField("email");
	public static final BooleanField VERIFIED = new BooleanField("verified");
	public static final BooleanField SUSPENDED = new BooleanField("suspended");
	public static final BooleanField SUPERUSER = new BooleanField("superuser");

	public User(ObjectNode node) {
		super(node);
	}

	public User(String id, String name) {
		this(id, name, new DateTime(DateTimeZone.UTC));
	}

	public User(String id, String name, DateTime created) {
		setValue(ID, id);
		setValue(NAME, name);
		setValue(CREATED, created);
	}

	public String getId() {
		return getValue(ID);
	}

	public String getName() {
		return getValue(NAME);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public String getPassword() {
		return getValue(PASSWORD);
	}

	public void setPassword(String password) {
		setValue(PASSWORD, password);
	}

	public boolean passwordEquals(String password) {
		return BCrypt.checkpw(password, getPassword());
	}

	public void changePassword(String password) {
		setPassword(BCrypt.hashpw(password, BCrypt.gensalt()));
	}

	public String getEmail() {
		return getValue(EMAIL);
	}

	public void setEmail(String email) {
		setValue(EMAIL, email);
	}

	public boolean isVerified() {
		return getValue(VERIFIED, false);
	}

	public void setVerified(boolean verified) {
		setValue(VERIFIED, verified);
	}

	public boolean isSuspended() {
		return getValue(SUSPENDED, false);
	}

	public void setSuspended(boolean suspended) {
		setValue(SUSPENDED, suspended);
	}

	public boolean isSuperuser() {
		return getValue(SUPERUSER, false);
	}

	public void setSuperuser(boolean superuser) {
		setValue(SUPERUSER, superuser);
	}

	public boolean equals(Identity identity) {
		return getId().equals(identity.getId());
	}

	@Override
	public String toString() {
		return getName();
	}

	public Identity asIdentity() {
		return new Identity(getId());
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(VERSION).add(ID).add(NAME)
			.add(CREATED).add(PASSWORD).add(EMAIL)
			.add(VERIFIED).add(SUSPENDED).add(SUPERUSER).build();
	}

	public User copy() {
		return new User(Nodes.copy(toJson()));
	}

	@Override
	public ObjectNode toJson() {
		return super.toJson();
	}
}
