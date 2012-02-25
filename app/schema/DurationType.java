package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;
import org.joda.time.Duration;

public class DurationType extends Type<Duration> {

	public DurationType() {
		super(Duration.class, "long");
	}

	@Override
	protected Duration get(JsonNode node) {
		return Duration.millis(node.getIntValue());
	}

	@Override
	protected JsonNode get(Duration value) {
		return new LongNode(value.getMillis());
	}
}
