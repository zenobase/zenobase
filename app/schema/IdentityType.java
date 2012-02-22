package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

import secure.Identity;

public class IdentityType extends Type<Identity> {

	public IdentityType() {
		super(Identity.class, "string");
	}

	@Override
	protected Identity get(JsonNode node) {
		return new Identity(node.asText());
	}

	@Override
	protected JsonNode get(Identity value) {
		return new TextNode(value.getId());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("index", "not_analyzed");
	}
}
