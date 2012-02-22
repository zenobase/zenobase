package models;

import org.codehaus.jackson.node.ObjectNode;

import schema.Field;
import schema.SchemaBuilder;
import schema.TextType;
import schema.TokenType;
import secure.Identity;
import services.IndexManager;

import com.google.common.base.Objects;
import com.google.common.collect.ImmutableSet;
import common.Nodes;

public class Bucket {

	public static final String TYPE_NAME = "bucket";
	public static final Field<Token> ID = Field.of("@id", new TokenType());
	public static final Field<Token> IDENTITY = Field.of("identity", new TokenType());
	public static final Field<Token> ROLE = Field.of("role", new TokenType());
	public static final Field<Text> LABEL = Field.of("label", new TextType());

	private static final ImmutableSet<Field<?>> FIELDS = ImmutableSet.of(ID, IDENTITY, ROLE, LABEL);

	private final IndexManager index;
	private final String id;
	private String label;
	private Identity identity;
	private String role;

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

	public Identity getIdentity() {
		return identity;
	}

	public void setIdentity(Identity identity) {
		this.identity = identity;
	}

	public String getRole() {
		return role;
	}

	public void setRole(String role) {
		this.role = role;
	}

	public void add(Event event) {
		index.index(Event.TYPE_NAME, event.getId(), event.getContent(), false);
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
		IDENTITY.getType().add(object, IDENTITY.getName(), Token.valueOf(identity.getId()));
		ROLE.getType().add(object, ROLE.getName(), Token.valueOf(role));
		LABEL.getType().add(object, LABEL.getName(), Text.valueOf(label));
		return object;
	}
}
