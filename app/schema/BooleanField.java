package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.BooleanNode;

public class BooleanField extends Field<Boolean> {

	public BooleanField(String name) {
		super(name, Boolean.class, "boolean");
	}

	@Override
	protected Boolean getValue(JsonNode node) {
		return node.asBoolean();
	}

	@Override
	protected JsonNode toJson(Boolean value) {
		return BooleanNode.valueOf(value);
	}
}
