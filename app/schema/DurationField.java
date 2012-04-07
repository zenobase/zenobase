package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.LongNode;
import org.joda.time.Duration;

public class DurationField extends Field<Duration> {

	public DurationField(String name) {
		super(name, Duration.class, "long");
	}

	@Override
	protected Duration get(JsonNode node) {
		return Duration.millis(node.getIntValue());
	}

	@Override
	protected JsonNode toJson(Duration value) {
		return new LongNode(value.getMillis());
	}
}
