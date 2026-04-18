package com.zenobase.tasks;

import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.MoreObjects;
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
import java.util.Objects;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import org.joda.time.Minutes;
import org.jspecify.annotations.Nullable;

public class Task extends DomainNode {

	public static final String TYPE_NAME = "task";

	public static final TokenField ID = new TokenField("@id");
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
		setValue(CREATED, DateTime.now(DateTimeZone.UTC));
	}

	@Override
	public String getId() {
		return Objects.requireNonNull(getValue(ID));
	}

	public String getType() {
		return Objects.requireNonNull(getValue(TYPE));
	}

	public String getBucketId() {
		return Objects.requireNonNull(getValue(BUCKET));
	}

	public Identity getPrincipal() {
		return Objects.requireNonNull(getValue(PRINCIPAL));
	}

	public DateTime getCreated() {
		return Objects.requireNonNull(getValue(CREATED));
	}

	public @Nullable DateTime getCompleted() {
		return getValue(COMPLETED);
	}

	public void setCompleted(DateTime completed) {
		setValue(COMPLETED, completed);
	}

	public @Nullable Status getStatus() {
		return getValue(STATUS);
	}

	public void setStatus(Status status) {
		setValue(STATUS, status);
	}

	public @Nullable String getMarker() {
		return getValue(MARKER);
	}

	public void setMarker(@Nullable String marker) {
		setValue(MARKER, marker);
	}

	public @Nullable String getUndoId() {
		return getValue(UNDO);
	}

	public void setUndoId(@Nullable String undoId) {
		setValue(UNDO, undoId);
	}

	public @Nullable ObjectNode getSettings() {
		return getValue(SETTINGS);
	}

	protected <T> @Nullable T getSetting(Field<T> field) {
		return getValue(SETTINGS, field);
	}

	protected <T> @Nullable Iterable<T> getSettings(Field<T> field) {
		return getValues(SETTINGS, field);
	}

	protected <T> void setSetting(Field<T> field, @Nullable T value) {
		setValue(SETTINGS, field, value);
	}

	protected <T> void setSettings(Field<T> field, Iterable<T> values) {
		setValues(SETTINGS, field, values);
	}

	public boolean isPermitted(Authorization auth) {
		return auth.getScope() == null && getPrincipal().equals(auth.getPrincipal());
	}

	public boolean isStale() {
		DateTime completed = MoreObjects.firstNonNull(getCompleted(), new DateTime(0L));
		return (
			getStatus() == Status.FAILED || Minutes.minutesBetween(completed, DateTime.now()).isGreaterThan(Minutes.ONE)
		);
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
		FAILED,
	}
}
