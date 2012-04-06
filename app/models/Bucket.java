package models;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Iterables;
import org.elasticsearch.common.collect.Lists;

import schema.Field;
import schema.ObjectType;
import schema.PermissionType;
import schema.SchemaBuilder;
import schema.TextType;
import schema.TokenType;
import secure.Identity;
import secure.Permission;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableMap.Builder;
import com.google.common.collect.ImmutableSet;
import common.Nodes;

public class Bucket {

	public static final String TYPE_NAME = "bucket";
	public static final Field<Token> ID = Field.of("@id", new TokenType());
	public static final Field<Text> LABEL = Field.of("label", new TextType());
	public static final Field<Text> DESCRIPTION = Field.of("description", new TextType());
	public static final Field<ObjectNode> PERMISSIONS = Field.of("permissions", new PermissionType());
	public static final Field<ObjectNode> WIDGETS = Field.of("widgets", new ObjectType());

	private static final ImmutableSet<Field<?>> FIELDS = ImmutableSet.of(ID, LABEL, DESCRIPTION, PERMISSIONS, WIDGETS);

	private final String id;
	private String label;
	private String description;
	private List<ObjectNode> permissions = Lists.newArrayList();
	private ImmutableList<ObjectNode> widgets = ImmutableList.of();

	public Bucket(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public String getLabel() {
		return label;
	}

	public void setLabel(String label) {
		this.label = label;
	}

	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	public ImmutableMap<Identity, Permission> getPermissions() {
		Builder<Identity, Permission> builder = ImmutableMap.builder();
		for (ObjectNode object : permissions) {
			Identity identity = Iterables.getOnlyElement(PermissionType.IDENTITY.getType().get(object, PermissionType.IDENTITY.getName()));
			Permission permission = Iterables.getOnlyElement(PermissionType.PERMISSION.getType().get(object, PermissionType.PERMISSION.getName()));
			builder.put(identity, permission);
		}
		return builder.build();
	}

	public Permission getPermission(Identity identity) {
		for (ObjectNode object : permissions) {
			if (PermissionType.IDENTITY.getType().get(object, PermissionType.IDENTITY.getName()).contains(identity)) {
				return Iterables.getOnlyElement(PermissionType.PERMISSION.getType().get(object, PermissionType.PERMISSION.getName()));
			}
		}
		return Permission.NONE;
	}

	public void setPermissions(Iterable<ObjectNode> permissions) {
		this.permissions = Lists.newArrayList(permissions);
	}

	public void grant(Identity identity, Permission permission) {
		ObjectNode object = Nodes.newObject();
		PermissionType.IDENTITY.getType().set(object, PermissionType.IDENTITY.getName(), identity);
		PermissionType.PERMISSION.getType().set(object, PermissionType.PERMISSION.getName(), permission);
		permissions.add(object);
	}

	public void setWidgets(Iterable<ObjectNode> widgets) {
		this.widgets = ImmutableList.copyOf(widgets);
	}

	public List<ObjectNode> getWidgets() {
		return widgets;
	}

	public static ObjectNode getSchema() {
		SchemaBuilder schema = new SchemaBuilder(TYPE_NAME);
		for (Field<?> field : FIELDS) {
			schema.add(field);
		}
		return schema.build();
	}

	@Override
	public String toString() {
		return Objects.firstNonNull(label, id);
	}

	public ObjectNode toJson() {
		ObjectNode object = Nodes.newObject();
		ID.getType().add(object, ID.getName(), Token.valueOf(id));
		LABEL.getType().add(object, LABEL.getName(), Text.valueOf(label));
		if (description != null) {
			DESCRIPTION.getType().add(object, DESCRIPTION.getName(), Text.valueOf(description));
		}
		PERMISSIONS.getType().add(object, PERMISSIONS.getName(), permissions);
		WIDGETS.getType().add(object, WIDGETS.getName(), widgets);
		return object;
	}

	public static Bucket parse(ObjectNode object) {
		String id = object.get(Bucket.ID.getName()).asText();
		Bucket bucket = new Bucket(id);
		bucket.setLabel(object.get(Bucket.LABEL.getName()).asText());
		if (object.has(Bucket.DESCRIPTION.getName())) {
			bucket.setDescription(object.get(Bucket.DESCRIPTION.getName()).asText());
		}
		bucket.setPermissions(Bucket.PERMISSIONS.getType().get(object, Bucket.PERMISSIONS.getName()));
		bucket.setWidgets(Bucket.WIDGETS.getType().get(object, Bucket.WIDGETS.getName()));
		return bucket;
	}
}
