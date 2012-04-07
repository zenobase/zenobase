package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;

public class LongField extends Field<Long> {

	public LongField(String name) {
		super(name, Long.class, "long");
	}

	@Override
	protected Long getValue(JsonNode node) {
		return node.asLong();
	}

	@Override
	protected JsonNode toJson(Long value) {
		return new LongNode(value);
	}
}
