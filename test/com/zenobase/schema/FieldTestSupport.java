package com.zenobase.schema;

import static org.fest.assertions.Assertions.assertThat;

import org.codehaus.jackson.node.ObjectNode;

import com.zenobase.common.Nodes;

public abstract class FieldTestSupport {

	protected final String FIELD_NAME = "field";

	protected <T> void roundtrip(Field<T> field, T value) {
		new SchemaBuilder("test").add(field).build().toJson(); // TODO put mapping
		ObjectNode node = Nodes.newObject();
		field.setValue(node, value);
		field.prePersist(node);
		// TODO: roundtrip to index
		field.postLoad(node);
		assertThat(field.getValue(node)).as("field value").isEqualTo(value);
		field.configureSchema(Nodes.newObject());
	}
}
