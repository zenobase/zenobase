package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import secure.Identity;
import secure.Permission;

public class PermissionType extends Type<ObjectNode> {

	public static final Field<Identity> IDENTITY = Field.of("identity", new IdentityType());
	public static final Field<Permission> PERMISSION = Field.of("permission", new EnumType<Permission>(Permission.class));

	public PermissionType() {
		super(ObjectNode.class, "nested");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, IDENTITY);
		configureSchema(properties, PERMISSION);
	}

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.getType().configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected ObjectNode get(JsonNode node) {
		return (ObjectNode) node;
	}

	@Override
	protected JsonNode get(ObjectNode value) {
		return value;
	}
}
