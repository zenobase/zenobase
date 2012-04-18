package models;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.node.ObjectNode;

import schema.ObjectField;
import schema.PermissionField;
import schema.Schema;
import schema.SchemaBuilder;
import schema.TextField;
import schema.TokenField;
import models.Identity;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class Bucket extends DomainNode {

	public static final String TYPE_NAME = "bucket";

	public static final TokenField ID = new TokenField("@id", false);
	public static final TextField LABEL = new TextField("label");
	public static final TextField DESCRIPTION = new TextField("description");
	public static final PermissionField PERMISSIONS = new PermissionField("permissions");
	public static final ObjectField WIDGETS = new ObjectField("widgets");

	public Bucket(ObjectNode object) {
		super(object);
	}

	public Bucket(String id) {
		setValue(ID, id);
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

	public ImmutableMap<Identity, Permission> getPermissions() {
		return toMap(getValues(PERMISSIONS));
	}

	private static <K, V> ImmutableMap<K, V> toMap(Iterable<Map.Entry<K, V>> entries) {
		ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
		for (Map.Entry<K, V> entry : entries) {
			builder.put(entry);
		}
		return builder.build();
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
