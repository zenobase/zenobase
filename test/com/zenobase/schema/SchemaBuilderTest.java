package com.zenobase.schema;

import java.io.IOException;

import org.junit.Test;

import com.zenobase.io.JsonPrinter;

public class SchemaBuilderTest {

	@Test
	public void test() throws IOException {
		SchemaBuilder builder = new SchemaBuilder("test")
			.add(new TokenField("who"))
			.add(new DateTimeField("when"))
			.add(new LocationField("where"));
		Schema schema = builder.build();
		builder.add(new TokenField("what"));
		new JsonPrinter(System.out).print(schema.toJson());
	}
}
