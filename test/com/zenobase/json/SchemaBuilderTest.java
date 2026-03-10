package com.zenobase.json;

import static com.zenobase.testing.NodeAssert.assertThat;
import static org.fest.assertions.Assertions.assertThat;

import org.junit.Test;

public class SchemaBuilderTest {

	@Test
	public void test() {

		String typeName = "test";
		TokenField field = new TokenField("who");
		SchemaBuilder builder = new SchemaBuilder(typeName).add(field);
		Schema s1 = builder.build();
		builder.add(new TokenField("what"));
		Schema s2 = builder.build();

		assertThat(s1.getTypeName()).as("type").isEqualTo(typeName);
		assertThat(s1.getField("who")).as("field").isEqualTo(field);
		assertThat(s1.toJson()).path("dynamic").isEqualTo("strict");
		assertThat(s1.toJson()).path("_source").path("excludes").isArray();
		assertThat(s1.toJson()).path("properties").path("who").isObject();
		assertThat(s1.toJson()).path("properties").path("what").isMissingNode();
		assertThat(s2.toJson()).path("properties").path("what").isObject();
	}
}
