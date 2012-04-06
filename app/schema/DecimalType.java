package schema;

import java.math.BigDecimal;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.DecimalNode;

public class DecimalType extends Type<BigDecimal> {

	public DecimalType() {
		super(BigDecimal.class, "double");
	}

	@Override
	protected BigDecimal get(JsonNode node) {
		return node.getDecimalValue();
	}

	@Override
	protected JsonNode toJson(BigDecimal value) {
		return new DecimalNode(value);
	}
}
