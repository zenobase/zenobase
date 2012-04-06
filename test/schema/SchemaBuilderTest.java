package schema;

import java.io.IOException;

import org.junit.Test;

import common.JsonPrinter;

public class SchemaBuilderTest {

	@Test
	public void test() throws IOException {
		SchemaBuilder builder = new SchemaBuilder("test")
			.add(Field.of("who", new TokenType()))
			.add(Field.of("when", new DateTimeType()))
			.add(Field.of("where", new LocationType()));
		Schema schema = builder.build();
		builder.add(Field.of("what", new TokenType()));
		new JsonPrinter(System.out).print(schema.toJson());
	}
}
