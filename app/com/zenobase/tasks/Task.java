package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Generator;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.EnumField;
import com.zenobase.json.Field;
import com.zenobase.json.IdentityField;
import com.zenobase.json.ObjectField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class Task extends DomainNode {

	public static final String TYPE_NAME = "bucket";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("type");
	public static final TokenField BUCKET = new TokenField("bucket");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final TokenField AUTHORIZATION_URL = new TokenField("authorizationUrl");

	public static final ObjectField CREDENTIALS = new ObjectField("credentials");
	public static final ObjectField SETTINGS = new ObjectField("settings");

	public static final DateTimeField COMPLETED = new DateTimeField("completed");
	public static final EnumField<Status> STATUS = EnumField.newInstance("status", Status.class);
	public static final TokenField MARKER = new TokenField("marker");
	public static final TokenField UNDO = new TokenField("undo");

	public Task(ObjectNode node) {
		super(node);
	}

	public Task(String type, String bucketId, Identity principal) {
		setValue(ID, Generator.id());
		setValue(TYPE, type);
		setValue(BUCKET, bucketId);
		setValue(PRINCIPAL, principal);
		setValue(CREATED, new DateTime(DateTimeZone.UTC));
	}

	public String getId() {
		return getValue(ID);
	}

	public String getType() {
		return getValue(TYPE);
	}

	public String getBucketId() {
		return getValue(BUCKET);
	}

	public Identity getPrincipal() {
		return getValue(PRINCIPAL);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public boolean isEnabled() {
		return getValue(AUTHORIZATION_URL) == null;
	}

	public String getAuthorizationUrl() {
		return getValue(AUTHORIZATION_URL);
	}

	public void setAuthorizationUrl(String authorizationUrl) {
		setValue(AUTHORIZATION_URL, authorizationUrl);
	}

	public DateTime getCompleted() {
		return getValue(COMPLETED);
	}

	public void setCompleted(DateTime completed) {
		setValue(COMPLETED, completed);
	}

	public Status getStatus() {
		return getValue(STATUS);
	}

	public void setStatus(Status status) {
		setValue(STATUS, status);
	}

	public String getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(String marker) {
		setValue(MARKER, marker);
	}

	protected <T> T getCredential(Field<T> field) {
		return getValue(CREDENTIALS, field);
	}

	protected <T> void setCredential(Field<T> field, T value) {
		setValue(CREDENTIALS, field, value);
	}

	public ObjectNode getSettings() {
		return getValue(SETTINGS);
	}

	protected <T> T getSetting(Field<T> field) {
		return getValue(SETTINGS, field);
	}

	protected <T> void setSetting(Field<T> field, T value) {
		setValue(SETTINGS, field, value);
	}

	public Task copy() {
		return copy(getClass());
	}

	/**
	 * Create a copy of this task with sensitive fields cleared.
	 */
	public Task sanitized() {
		Task sanitized = copy();
		sanitized.setValue(CREDENTIALS, null);
		return sanitized;
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(VERSION).add(ID).add(TYPE).add(BUCKET).add(PRINCIPAL).add(CREATED).add(AUTHORIZATION_URL)
			.add(COMPLETED).add(STATUS).add(MARKER)
			.add(CREDENTIALS).add(SETTINGS).build();
	}

	public enum Status {
		SUCCESS,
		FAILED
	}
}
