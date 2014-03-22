package com.zenobase.tasks;

import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Objects;

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
import com.zenobase.oauth.Authorization;

public class Task extends DomainNode {

	public static final String TYPE_NAME = "task";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("type");
	public static final TokenField BUCKET = new TokenField("bucket");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField CREATED = new DateTimeField("created");
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

	public String getUndoId() {
		return getValue(UNDO);
	}

	public void setUndoId(String undoId) {
		setValue(UNDO, undoId);
	}

	public ObjectNode getSettings() {
		return getValue(SETTINGS);
	}

	protected <T> T getSetting(Field<T> field) {
		return getValue(SETTINGS, field);
	}

	protected <T> Iterable<T> getSettings(Field<T> field) {
		return getValues(SETTINGS, field);
	}

	protected <T> void setSetting(Field<T> field, T value) {
		setValue(SETTINGS, field, value);
	}

	protected <T> void setSettings(Field<T> field, Iterable<T> values) {
		setValues(SETTINGS, field, values);
	}

	public boolean isPermitted(Authorization auth) {
		return auth.getScope() == null
			&& getPrincipal().equals(auth.getPrincipal());
	}

	public boolean isStale() {
		DateTime completed = Objects.firstNonNull(getCompleted(), new DateTime(0L));
		return getStatus() == Status.FAILED || Minutes.minutesBetween(completed, DateTime.now()).isGreaterThan(Minutes.ONE);
	}

	public Task copy() {
		return copy(getClass());
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(VERSION)
			.add(ID)
			.add(TYPE)
			.add(BUCKET)
			.add(PRINCIPAL)
			.add(CREATED)
			.add(COMPLETED)
			.add(STATUS)
			.add(MARKER)
			.add(UNDO)
			.add(SETTINGS)
			.build();
	}

	public enum Status {
		SUCCESS,
		FAILED
	}
}
