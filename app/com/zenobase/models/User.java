package com.zenobase.models;

import com.fasterxml.jackson.databind.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.BCrypt;
import com.zenobase.common.Generator;
import com.zenobase.json.BooleanField;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.IntegerField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;

public class User extends DomainNode {

	public static final String TYPE_NAME = "user";

	public static final TokenField ID = new TokenField("@id");
	public static final TokenField NAME = new TokenField("name");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField PASSWORD = new TokenField("password", false);
	public static final TokenField EMAIL = new TokenField("email");
	public static final BooleanField VERIFIED = new BooleanField("verified");
	public static final BooleanField SUSPENDED = new BooleanField("suspended");
	public static final BooleanField SUPERUSER = new BooleanField("superuser");
	public static final BooleanField OPTEDOUT = new BooleanField("optedout");
	public static final IntegerField QUOTA = new IntegerField("quota");

	public User(ObjectNode node) {
		super(node);
	}

	public User(String name) {
		this(Generator.id(), name);
	}

	public User(String id, String name) {
		this(id, name, new DateTime(DateTimeZone.UTC));
	}

	public User(String id, String name, DateTime created) {
		setValue(ID, id);
		setValue(NAME, name);
		setValue(CREATED, created);
	}

	@Override
	public String getId() {
		return getValue(ID);
	}

	public String getName() {
		return getValue(NAME);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public String getHashedPassword() {
		return getValue(PASSWORD);
	}

	public static String hashPassword(String password) {
		return BCrypt.hashpw(password);
	}

	public void setHashedPassword(String hashed) {
		setValue(PASSWORD, hashed);
	}

	public void setPassword(String password) {
		setHashedPassword(hashPassword(password));
	}

	public boolean passwordEquals(String password) {
		return BCrypt.checkpw(password, getHashedPassword());
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

	public boolean isOptedOut() {
		return getValue(OPTEDOUT, false);
	}

	public void setOptedOut(boolean optedOut) {
		setValue(OPTEDOUT, optedOut);
	}

	public boolean is(Identity identity) {
		return getId().equals(identity.getId());
	}

	public Integer getQuota() {
		return getValue(QUOTA);
	}

	public void setQuota(Integer quota) {
		setValue(QUOTA, quota);
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
			.add(VERIFIED).add(SUSPENDED).add(SUPERUSER).add(OPTEDOUT)
			.add(QUOTA).build();
	}

	public User copy() {
		return new User(toJson().deepCopy());
	}
}
