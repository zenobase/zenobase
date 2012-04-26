package com.zenobase.schema;

import static com.zenobase.test.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import java.io.IOException;

import org.junit.Test;

public class SchemaBuilderTest {

	@Test
	public void test() throws IOException {

		final String typeName = "test";
		SchemaBuilder builder = new SchemaBuilder(typeName).add(new TokenField("who"));
		Schema s1 = builder.build();
		builder.add(new TokenField("what"));
		Schema s2 = builder.build();

		assertThat(s1.getTypeName()).as("type").isEqualTo(typeName);
		assertThat(s1.toJson()).path(typeName).path("dynamic").isEqualTo("strict");
		assertThat(s1.toJson()).path(typeName).path("_source").path("excludes").isArray();
		assertThat(s1.toJson()).path(typeName).path("_type").path("index").isEqualTo("no");
		assertThat(s1.toJson()).path(typeName).path("_all").path("enabled").isEqualTo(false);
		assertThat(s1.toJson()).path(typeName).path("properties").path("who").isObject();
		assertThat(s1.toJson()).path(typeName).path("properties").path("what").isMissingNode();
		assertThat(s2.toJson()).path(typeName).path("properties").path("what").isObject();
	}
}
