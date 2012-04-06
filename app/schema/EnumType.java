package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class EnumType<E extends Enum<E>> extends Type<E> {

	public EnumType(Class<E> type) {
		super(type, "string");
	}

	@Override
	protected E get(JsonNode node) {
		return Enum.valueOf((Class<E>) getType(), node.asText());
	}

	@Override
	protected JsonNode get(E value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
