package com.zenobase.schema;

import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class SchemaBuilderTest {

	@Test
	public void test() throws IOException {
		SchemaBuilder builder = new SchemaBuilder("test")
			.add(new TokenField("who"))
			.add(new DateTimeField("when"))
			.add(new LocationField("where"));
		Schema schema = builder.build();
		builder.add(new TokenField("what"));
		assertThat(schema.toJson().has("what")).as("has what").isFalse();
	}
}
