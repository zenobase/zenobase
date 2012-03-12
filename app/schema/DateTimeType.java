package schema;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;
import org.codehaus.jackson.node.TextNode;
import org.joda.time.DateTime;
import org.joda.time.format.DateTimeFormatter;
import org.joda.time.format.ISODateTimeFormat;

public class DateTimeType extends Type<DateTime> {

	private final DateTimeFormatter formatter = ISODateTimeFormat.dateTime().withOffsetParsed();

	public DateTimeType() {
		super(DateTime.class, "date");
	}

	@Override
	protected DateTime get(JsonNode node) {
		return formatter.parseDateTime(node.getTextValue());
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
