package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;
import org.codehaus.jackson.node.ObjectNode;

public class LongField extends Field<Long> {

	private final boolean indexed;

	public LongField(String name) {
		this(name, true);
	}

	public LongField(String name, boolean indexed) {
		super(name, Long.class, "long");
		this.indexed = indexed;
	}

	@Override
	protected Long getValue(JsonNode node) {
		return node.asLong();
	}

	@Override
	protected JsonNode toJson(Long value) {
		return new LongNode(value);
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		if (!indexed) {
			schema.put("index", "no");
		}
	}
}
