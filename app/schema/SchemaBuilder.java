package schema;

import org.codehaus.jackson.node.ObjectNode;

import common.Nodes;

public class SchemaBuilder {

	private static final String PROPERTIES = "properties";

	private final ObjectNode schema = Nodes.newObject();
	private final ObjectNode type;
	private final ObjectNode properties;

	public SchemaBuilder(String typeName) {
		type = schema.putObject(typeName);
		type.put("dynamic", "strict");
		properties = type.putObject(PROPERTIES);
	}

	public SchemaBuilder add(Field<?> field) {
		field.getType().configureSchema(properties.putObject(field.getName()));
		return this;
	}

	public SchemaBuilder index(boolean b) {
		type.putObject("_index").put("enabled", b);
		return this;
	}

	public ObjectNode build() {
		return Nodes.copy(schema);
	}
}
