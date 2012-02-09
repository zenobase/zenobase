package schema;

import models.Length;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.DecimalNode;

public class LengthType extends Type<Length> {

	public LengthType() {
		super(Length.class, "float");
	}

	@Override
	protected Length get(JsonNode node) {
		return Length.valueOf(node.getDecimalValue(), Length.Unit.m);
	}

	@Override
	protected JsonNode get(Length value) {
		return new DecimalNode(value.getValue()); // TODO normalize unit if not m
	}
}
