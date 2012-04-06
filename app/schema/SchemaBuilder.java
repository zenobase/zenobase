package schema;

import org.codehaus.jackson.node.ObjectNode;

import common.Nodes;

public class SchemaBuilder {

	private static final String PROPERTIES = "properties";

	private final String typeName;
	private final ObjectNode schema = Nodes.newObject();
	private final ObjectNode type;
	private final ObjectNode properties;

	public SchemaBuilder(String typeName) {
		this.typeName = typeName;
		this.type = schema.putObject(typeName);
		this.type.put("dynamic", "strict");
		this.properties = type.putObject(PROPERTIES);
	}

	public SchemaBuilder add(Field<?> field) {
		field.getType().configureSchema(properties.putObject(field.getName()));
		return this;
	}

	public Schema build() {
		return new Schema(typeName, Nodes.copy(schema));
	}
}
