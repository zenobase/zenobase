package com.zenobase.models;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import org.codehaus.jackson.node.ObjectNode;
import org.joda.time.DateTime;
import org.joda.time.DateTimeZone;
import com.google.common.base.Objects;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import com.zenobase.common.Generator;
import com.zenobase.json.DateTimeField;
import com.zenobase.json.DomainNode;
import com.zenobase.json.Nodes;
import com.zenobase.json.ObjectField;
import com.zenobase.json.PermissionField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;

public class Bucket extends DomainNode {

	private static final Pattern LABEL_PATTERN = Pattern.compile("[a-zA-Z0-9-_ ]{1,20}");

	public static final String TYPE_NAME = "bucket";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TextField LABEL = new TextField("label");
	public static final TextField DESCRIPTION = new TextField("description");
	public static final DateTimeField CREATED = new DateTimeField("created");
	public static final PermissionField PERMISSIONS = new PermissionField("permissions");
	public static final ObjectField WIDGETS = new ObjectField("widgets");

	public Bucket(ObjectNode node) {
		super(node);
	}

	public Bucket() {
		setValue(ID, Generator.id());
		setValue(CREATED, new DateTime(DateTimeZone.UTC));
	}

	public String getId() {
		return getValue(ID);
	}

	public String getLabel() {
		return getValue(LABEL);
	}

	public void setLabel(String label) {
		setValue(LABEL, label);
	}

	public String getDescription() {
		return getValue(DESCRIPTION);
	}

	public void setDescription(String description) {
		setValue(DESCRIPTION, description);
	}

	public DateTime getCreated() {
		return getValue(CREATED);
	}

	public ImmutableSet<Identity> getPrincipals(Permission permission) {
		ImmutableSet.Builder<Identity> principals = ImmutableSet.builder();
		for (Map.Entry<Identity, Permission> entry : getValues(PERMISSIONS)) {
			if (entry.getValue() == permission) {
				principals.add(entry.getKey());
			}
		}
		return principals.build();
	}

	public Permission getPermission(Identity principal) {
		for (Map.Entry<Identity, Permission> entry : getValues(PERMISSIONS)) {
			if (entry.getKey().equals(principal)) {
				return entry.getValue();
			}
		}
		return Permission.NONE;
	}

	public void addPermission(Identity principal, Permission permission) {
		addValue(PERMISSIONS, Maps.immutableEntry(principal, permission));
	}

	public List<ObjectNode> getWidgets() {
		return getValues(WIDGETS);
	}

	public void setWidgets(Iterable<ObjectNode> widgets) {
		setValues(WIDGETS, widgets);
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME).add(VERSION)
			.add(ID).add(LABEL).add(DESCRIPTION).add(CREATED)
			.add(PERMISSIONS).add(WIDGETS).build();
	}

	public Bucket copy() {
		return new Bucket(Nodes.copy(toJson()));
	}

	public boolean valid() {
		return !Strings.isNullOrEmpty(getLabel()) &&
			LABEL_PATTERN.matcher(getLabel().trim()).matches() &&
			!getPrincipals(Permission.ALL).isEmpty();
	}

	@Override
	public String toString() {
		return Objects.firstNonNull(getLabel(), getId());
	}
}
