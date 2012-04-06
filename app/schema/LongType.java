package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;

public class LongType extends Type<Long> {

	public LongType() {
		super(Long.class, "long");
	}

	@Override
	protected Long get(JsonNode node) {
		return node.asLong();
	}

	@Override
	protected JsonNode toJson(Long value) {
		return new LongNode(value);
	}
}
