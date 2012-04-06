package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.BooleanNode;

public class BooleanType extends Type<Boolean> {

	public BooleanType() {
		super(Boolean.class, "boolean");
	}

	@Override
	protected Boolean get(JsonNode node) {
		return node.asBoolean();
	}

	@Override
	protected JsonNode toJson(Boolean value) {
		return BooleanNode.valueOf(value);
	}
}
