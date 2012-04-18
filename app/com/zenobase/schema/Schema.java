package com.zenobase.schema;

import org.codehaus.jackson.node.ObjectNode;

public class Schema {

	private final String typeName;
	private final ObjectNode schema;

	public Schema(String typeName, ObjectNode schema) {
		this.typeName = typeName;
		this.schema = schema;
	}

	public String getTypeName() {
		return typeName;
	}

	@Override
	public String toString() {
		return typeName;
	}

	public ObjectNode toJson() {
		return schema;
	}

}
