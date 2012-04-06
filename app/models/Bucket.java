package models;

import java.util.List;

import org.codehaus.jackson.node.ObjectNode;
import org.elasticsearch.common.collect.Lists;

import schema.Field;
import schema.ObjectType;
import schema.RoleType;
import schema.SchemaBuilder;
import schema.TextType;
import schema.TokenType;
import secure.Identity;
import secure.Role;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import common.Nodes;

public class Bucket {

	public static final String TYPE_NAME = "bucket";
	public static final Field<Token> ID = Field.of("@id", new TokenType());
	public static final Field<Text> LABEL = Field.of("label", new TextType());
	public static final Field<Text> DESCRIPTION = Field.of("description", new TextType());
	public static final Field<Role> ROLE = Field.of("roles", new RoleType());
	public static final Field<ObjectNode> WIDGET = Field.of("widgets", new ObjectType());

	private static final ImmutableSet<Field<?>> FIELDS = ImmutableSet.of(ID, LABEL, DESCRIPTION, ROLE, WIDGET);

	private final String id;
	private String label;
	private String description;
	private final List<Role> roles = Lists.newArrayList();
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

	public String getRole(Identity identity) {
		for (Role role : roles) {
			if (role.getIdentity().equals(identity)) {
				return role.getRole();
			}
		}
		return null;
	}

	public Identity getIdentity(String role) {
		for (Role r : roles) {
			if (r.getRole().equals(role)) {
				return r.getIdentity();
			}
		}
		return null;
	}

	public void addRole(Role role) {
		roles.add(role);
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
		ROLE.getType().add(object, ROLE.getName(), roles);
		WIDGET.getType().add(object, WIDGET.getName(), widgets);
		return object;
	}

	public static Bucket parse(ObjectNode object) {
		String id = object.get(Bucket.ID.getName()).asText();
		Bucket bucket = new Bucket(id);
		bucket.setLabel(object.get(Bucket.LABEL.getName()).asText());
		if (object.has(Bucket.DESCRIPTION.getName())) {
			bucket.setDescription(object.get(Bucket.DESCRIPTION.getName()).asText());
		}
		for (Role role : Bucket.ROLE.getType().get(object, Bucket.ROLE.getName())) {
			bucket.addRole(role);
		}
		bucket.setWidgets(Bucket.WIDGET.getType().get(object, Bucket.WIDGET.getName()));
		return bucket;
	}
}
