package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class TokenType extends Type<String> {

	public TokenType() {
		super(String.class, "string");
	}

	@Override
	protected String get(JsonNode node) {
		return node.asText();
	}

	@Override
	protected JsonNode toJson(String value) {
		return new TextNode(value);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
