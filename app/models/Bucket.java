package models;

import java.util.List;
import java.util.Map;

import org.codehaus.jackson.node.ObjectNode;

import schema.Field;
import schema.ObjectType;
import schema.PermissionType;
import schema.Schema;
import schema.SchemaBuilder;
import schema.TextType;
import secure.Identity;
import secure.Permission;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class Bucket extends DomainNode {

	public static final String TYPE_NAME = "bucket";
	public static final Field<String> LABEL = Field.of("label", new TextType());
	public static final Field<String> DESCRIPTION = Field.of("description", new TextType());
	public static final Field<Map.Entry<Identity, Permission>> PERMISSIONS = Field.of("permissions", new PermissionType());
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
		setValue(LABEL, label);
	}

	public String getDescription() {
		return getValue(DESCRIPTION);
	}

	public void setDescription(String description) {
		setValue(DESCRIPTION, description);
	}

	public ImmutableMap<Identity, Permission> getPermissions() {
		ImmutableMap.Builder<Identity, Permission> builder = ImmutableMap.builder();
		for (Map.Entry<Identity, Permission> entry : getValues(PERMISSIONS)) {
			builder.put(entry);
		}
		return builder.build();
	}

	public Permission getPermission(Identity identity) {
		for (Map.Entry<Identity, Permission> entry : getValues(PERMISSIONS)) {
			if (entry.getKey().equals(identity)) {
				return entry.getValue();
			}
		}
		return Permission.NONE;
	}

	public void addPermission(Identity identity, Permission permission) {
		addValue(PERMISSIONS, Maps.immutableEntry(identity, permission));
	}
	
	public List<ObjectNode> getWidgets() {
		return getValues(WIDGETS);
	}

	public void setWidgets(Iterable<ObjectNode> widgets) {
		setValues(WIDGETS, widgets);
	}

	public static Schema getSchema() {
		return new SchemaBuilder(TYPE_NAME)
			.add(ID).add(LABEL).add(DESCRIPTION)
			.add(PERMISSIONS).add(WIDGETS).build();
	}

	@Override
	public String toString() {
		return getLabel();
	}
}
