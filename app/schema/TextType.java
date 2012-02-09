package schema;

import models.Text;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class TextType extends Type<Text> {

	public TextType() {
		super(Text.class, "string");
	}

	@Override
	protected Text get(JsonNode node) {
		return Text.valueOf(node.asText());
	}

	@Override
	protected JsonNode get(Text value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "analyzed");
	}
}
