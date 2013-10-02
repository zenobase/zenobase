package com.zenobase.json;

import static org.fest.assertions.Assertions.assertThat;

import com.fasterxml.jackson.databind.node.ObjectNode;

public abstract class FieldTestSupport {

	protected final String FIELD_NAME = "field";

	protected <T> void roundtrip(Field<T> field, T value) {
		new SchemaBuilder("test").add(field).build().toJson(); // TODO put mapping
		ObjectNode node = Nodes.newObject();
		field.setValue(node, value);
		field.prePersist(node);
		// TODO: roundtrip to index
		assertThat(field.getValue(node)).as("field value").isEqualTo(value);
		field.configureSchema(Nodes.newObject());
	}
}
