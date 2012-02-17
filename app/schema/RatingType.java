package schema;

import models.Rating;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;

public class RatingType extends Type<Rating> {

	public RatingType() {
		super(Rating.class, "byte");
	}

	@Override
	protected Rating get(JsonNode node) {
		return Rating.valueOf(node.asInt());
	}

	@Override
	protected JsonNode get(Rating value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("precision_step", "0");
	}
}
