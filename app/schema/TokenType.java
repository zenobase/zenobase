package schema;

import models.Token;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class TokenType extends Type<Token> {

	public TokenType() {
		super(Token.class, "string");
	}

	@Override
	protected Token get(JsonNode node) {
		return Token.valueOf(node.asText());
	}

	@Override
	protected JsonNode get(Token value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
