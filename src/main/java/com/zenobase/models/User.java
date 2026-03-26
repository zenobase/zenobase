package com.zenobase.models;

import java.util.Objects;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.jspecify.annotations.Nullable;
import org.mindrot.jbcrypt.BCrypt;

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
		this(id, name, DateTime.now(DateTimeZone.UTC));
	}

	public User(String id, @Nullable String name, @Nullable DateTime created) {
		setValue(ID, id);
		setValue(NAME, name);
		setValue(CREATED, created);
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public @Nullable String getName() {
		return getValue(NAME);
	}

	public @Nullable DateTime getCreated() {
		return getValue(CREATED);
	}

	public @Nullable String getHashedPassword() {
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
		String hashed = getHashedPassword();
		return hashed != null && BCrypt.checkpw(password, hashed);
	}

	public @Nullable String getEmail() {
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

	public @Nullable Integer getQuota() {
		return getValue(QUOTA);
	}

	public void setQuota(@Nullable Integer quota) {
		setValue(QUOTA, quota);
	}

	@Override
	public String toString() {
		return MoreObjects.firstNonNull(getName(), getId());
	}

	public Identity asIdentity() {
		return new Identity(getId());
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
				.add(VERSION)
				.add(ID)
				.add(NAME)
				.add(CREATED)
				.add(PASSWORD)
				.add(EMAIL)
				.add(VERIFIED)
				.add(SUSPENDED)
				.add(SUPERUSER)
				.add(OPTEDOUT)
				.add(QUOTA)
				.build();
	}

	public User copy() {
		return new User(toJson().deepCopy());
	}
}
