package com.zenobase.models;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.node.ObjectNode;
import com.google.common.collect.ImmutableSet;
import com.google.common.collect.Maps;

import com.zenobase.common.Generator;
import com.zenobase.json.ObjectField;
import com.zenobase.json.PermissionField;
import com.zenobase.json.Schema;
import com.zenobase.json.SchemaBuilder;
import com.zenobase.json.TextField;
import com.zenobase.json.TokenField;

public class Bucket extends DomainNode {

	public static final String TYPE_NAME = "bucket";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TextField LABEL = new TextField("label");
	public static final TextField DESCRIPTION = new TextField("description");
	public static final PermissionField PERMISSIONS = new PermissionField("permissions");
	public static final ObjectField WIDGETS = new ObjectField("widgets");

	public Bucket(ObjectNode node) {
		super(node);
	}

	public Bucket() {
		setValue(ID, Generator.id());
	}

	public String getId() {
		return getValue(ID);
	}

	public String getLabel() {
		return getValue(LABEL).toString();
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
			.add(ID).add(LABEL).add(DESCRIPTION)
			.add(PERMISSIONS).add(WIDGETS).build();
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
