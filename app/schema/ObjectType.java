package schema;

import models.Resource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

public class ObjectType extends Type<ObjectNode> {

	public ObjectType() {
		super(Resource.class, "object");
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("enabled", false);
	}

	@Override
	protected ObjectNode get(JsonNode node) {
		return (ObjectNode) node;
	}

	@Override
	protected JsonNode toJson(ObjectNode value) {
		return value;
	}
}
