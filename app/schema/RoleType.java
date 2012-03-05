package schema;

import models.Token;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import secure.Identity;
import secure.Role;

import com.google.common.collect.Iterables;
import common.Nodes;

public class RoleType extends Type<Role> {

	public static final Field<Identity> IDENTITY = Field.of("identity", new IdentityType());
	public static final Field<Token> ROLE = Field.of("role", new TokenType());

	public RoleType() {
		super(Role.class, "nested");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, IDENTITY);
		configureSchema(properties, ROLE);
	}

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.getType().configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected Role get(JsonNode node) {
		return get((ObjectNode) node);
	}

	private static Role get(ObjectNode node) {
		return new Role(get((ObjectNode) node, IDENTITY), get((ObjectNode) node, ROLE).toString());
	}

	private static <T> T get(JsonNode node, Field<T> field) {
		return Iterables.getOnlyElement(field.getType().get((ObjectNode) node, field.getName()));
	}

	@Override
	protected JsonNode get(Role value) {
		ObjectNode object = Nodes.newObject();
		add(object, IDENTITY, value.getIdentity());
		add(object, ROLE, Token.valueOf(value.getRole()));
		return object;
	}

	private static <T> void add(ObjectNode object, Field<T> field, T value) {
		field.getType().add(object, field.getName(), value);
	}
}
