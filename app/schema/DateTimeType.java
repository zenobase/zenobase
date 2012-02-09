package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.DateTime;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeType extends Type<DateTime> {

	public DateTimeType() {
		super(DateTime.class, "date");
	}

	@Override
	protected DateTime get(JsonNode node) {
		return ISODateTimeFormat.dateTime().parseDateTime(node.getTextValue());
	}

	@Override
	protected JsonNode get(DateTime value) {
		return new TextNode(value.toString());
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		schema.put("format", "date_time");
	}
}
