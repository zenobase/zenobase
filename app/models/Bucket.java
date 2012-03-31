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
import services.IndexManager;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import common.Nodes;

public class Bucket {

	public static final String TYPE_NAME = "bucket";
	public static final Field<Token> ID = Field.of("@id", new TokenType());
	public static final Field<Text> LABEL = Field.of("label", new TextType());
	public static final Field<Role> ROLE = Field.of("roles", new RoleType());
	public static final Field<ObjectNode> WIDGET = Field.of("widgets", new ObjectType());

	private static final ImmutableSet<Field<?>> FIELDS = ImmutableSet.of(ID, LABEL, ROLE, WIDGET);

	private final IndexManager index;
	private final String id;
	private String label;
	private final List<Role> roles = Lists.newArrayList();
	private final List<ObjectNode> widgets = Lists.newArrayList();

	public Bucket(IndexManager index, String id) {
		this.index = index;
		this.id = id;
	}

	public String getId() {
		return id;
	}

	public void setLabel(String label) {
		this.label = label;
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

	public void addWidget(ObjectNode widget) {
		widgets.add(widget);
	}

	public void add(Event event) {
		event.prePersist();
		index.store(Event.TYPE_NAME, event.getId(), event.getContent(), false);
	}

	public void delete(String eventId) {
		index.delete(Event.TYPE_NAME, eventId);
	}

	public Event findEvent(String eventId) {
		ObjectNode object = index.get(Event.TYPE_NAME, eventId);
		return object != null ? new Event(eventId, id, object) : null;
	}

	public long getSize() {
		return index.count();
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
		ROLE.getType().add(object, ROLE.getName(), roles);
		WIDGET.getType().add(object, WIDGET.getName(), widgets);
		return object;
	}
}
