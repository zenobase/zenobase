package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class TextType extends Type<String> {

	public TextType() {
		super(String.class, "string");
	}

	@Override
	protected String get(JsonNode node) {
		return node.asText();
	}

	@Override
	protected JsonNode get(String value) {
		return new TextNode(value);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "analyzed");
	}
}
