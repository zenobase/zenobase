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
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TokenField;
import com.zenobase.models.Identity;

public class Task extends DomainNode {

	public static final String TYPE_NAME = "bucket";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TokenField TYPE = new TokenField("type");
	public static final EnumField<State> STATE = EnumField.newInstance("state", State.class);
	public static final TokenField BUCKET = new TokenField("bucket");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final DateTimeField UPDATED = new DateTimeField("updated");
	public static final ObjectField CONFIG = new ObjectField("config");

	public Task(ObjectNode node) {
		super(node);
	}

	public Task(String bucketId, String type, Identity principal) {
		this(Generator.id(), type, State.SUSPENDED, bucketId, principal);
	}

	public Task(String id, String type, State state, String bucketId, Identity principal) {
		setValue(ID, id);
		setValue(TYPE, type);
		setValue(STATE, state);
		setValue(BUCKET, bucketId);
		setValue(PRINCIPAL, principal);
		DateTime timestamp = new DateTime(DateTimeZone.UTC);
		setValue(CREATED, timestamp);
		setValue(UPDATED, timestamp);
		setValue(CONFIG, Nodes.newObject());
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

	public State getState() {
		return getValue(STATE);
	}

	public void setState(State state) {
		setValue(STATE, state);
	}

	public Identity getPrincipal() {
		return getValue(PRINCIPAL);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public DateTime getUpdated() {
		return getValue(UPDATED);
	}

	public void setUpdated(DateTime updated) {
		setValue(UPDATED, updated);
	}

	public <T> T getConfigValue(Field<T> field) {
		return field.getValue(getValue(CONFIG));
	}

	public <T> void setConfigValue(Field<T> field, T value) {
		field.setValue(getValue(CONFIG), value);
	}

	public Task copy() {
		return new Task(Nodes.copy(toJson()));
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(VERSION).add(ID).add(TYPE).add(STATE)
			.add(BUCKET).add(PRINCIPAL)
			.add(CREATED).add(UPDATED)
			.add(CONFIG).build();
	}

	public enum State {
		READY,
		SUSPENDED
	}
}
