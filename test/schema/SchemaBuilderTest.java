package schema;

import java.io.IOException;

import org.codehaus.jackson.node.ObjectNode;
import org.junit.Test;

import schema.DateTimeType;
import schema.Field;
import schema.LocationType;
import schema.SchemaBuilder;
import schema.TokenType;

import common.JsonPrinter;

public class SchemaBuilderTest {

	@Test
	public void test() throws IOException {
		SchemaBuilder builder = new SchemaBuilder("test")
			.add(Field.of("who", new TokenType()))
			.add(Field.of("when", new DateTimeType()))
			.add(Field.of("where", new LocationType()));
		ObjectNode schema = builder.build();
		builder.add(Field.of("what", new TokenType()));
		new JsonPrinter(System.out).print(schema);
	}
}
