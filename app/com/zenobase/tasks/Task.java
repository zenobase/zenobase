package com.zenobase.tasks;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;

import com.zenobase.common.Generator;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
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
	public static final TokenField BUCKET = new TokenField("bucket");
	public static final IdentityField PRINCIPAL = new IdentityField("principal");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final DateTimeField MODIFIED = new DateTimeField("modified");
	public static final ObjectField CONFIG = new ObjectField("config");

	public Task(ObjectNode node) {
		super(node);
	}

	public Task(String bucketId, String type, Identity principal) {
		this(Generator.id(), type, bucketId, principal);
	}

	public Task(String id, String type, String bucketId, Identity principal) {
		setValue(ID, id);
		setValue(TYPE, type);
		setValue(BUCKET, bucketId);
		setValue(PRINCIPAL, principal);
		DateTime timestamp = new DateTime(DateTimeZone.UTC);
		setValue(CREATED, timestamp);
		setValue(MODIFIED, timestamp);
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

	public Identity getPrincipal() {
		return getValue(PRINCIPAL);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public DateTime getModified() {
		return getValue(MODIFIED);
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
			.add(VERSION).add(ID).add(TYPE)
			.add(BUCKET).add(PRINCIPAL)
			.add(CREATED).add(MODIFIED)
			.add(CONFIG).build();
	}
}
