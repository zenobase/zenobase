package models;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;

import schema.Field;
import schema.ObjectType;
import schema.PermissionType;
import schema.SchemaBuilder;
import schema.TextType;
import secure.Identity;
import secure.Permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import common.Nodes;

public class Bucket extends DomainNode {

	public static final String TYPE_NAME = "bucket";
	public static final Field<Text> LABEL = Field.of("label", new TextType());
	public static final Field<Text> DESCRIPTION = Field.of("description", new TextType());
	public static final Field<ObjectNode> PERMISSIONS = Field.of("permissions", new PermissionType());
	public static final Field<ObjectNode> WIDGETS = Field.of("widgets", new ObjectType());

	public Bucket(ObjectNode object) {
		super(object);
	}

	public Bucket(String id) {
		super(id);
	}

	public String getLabel() {
		return getValue(LABEL).toString();
	}

	public void setLabel(String label) {
		setValue(LABEL, Text.valueOf(label));
	}

	public String getDescription() {
		return getValue(DESCRIPTION).toString();
	}

	public void setDescription(String description) {
		setValue(DESCRIPTION, Text.valueOf(description));
	}

	public ImmutableMap<Identity, Permission> getPermissions() {
		Builder<Identity, Permission> builder = ImmutableMap.builder();
		for (ObjectNode object : getValues(PERMISSIONS)) {
			Identity identity = Iterables.getOnlyElement(PermissionType.IDENTITY.getType().get(object, PermissionType.IDENTITY.getName()));
			Permission permission = Iterables.getOnlyElement(PermissionType.PERMISSION.getType().get(object, PermissionType.PERMISSION.getName()));
			builder.put(identity, permission);
		}
		return builder.build();
	}

	public Permission getPermission(Identity identity) {
		for (ObjectNode object : getValues(PERMISSIONS)) {
			if (PermissionType.IDENTITY.getType().get(object, PermissionType.IDENTITY.getName()).contains(identity)) {
				return Iterables.getOnlyElement(PermissionType.PERMISSION.getType().get(object, PermissionType.PERMISSION.getName()));
			}
		}
		return Permission.NONE;
	}

	public void setPermissions(Iterable<ObjectNode> permissions) {
		setValues(PERMISSIONS, permissions);
	}

	public void addPermission(Identity identity, Permission permission) {
		ObjectNode object = Nodes.newObject();
		PermissionType.IDENTITY.getType().set(object, PermissionType.IDENTITY.getName(), identity);
		PermissionType.PERMISSION.getType().set(object, PermissionType.PERMISSION.getName(), permission);
		addValue(PERMISSIONS, object);
	}
	
	public List<ObjectNode> getWidgets() {
		return getValues(WIDGETS);
	}

	public void setWidgets(Iterable<ObjectNode> widgets) {
		setValues(WIDGETS, widgets);
	}

	public static ObjectNode getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(ID).add(LABEL).add(DESCRIPTION)
			.add(PERMISSIONS).add(WIDGETS).build();
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
