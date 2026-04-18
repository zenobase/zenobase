package com.zenobase.commands;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.zenobase.json.Field;

public class Migration {

	public static <T> void copy(Field<T> field, ObjectNode from, ObjectNode to) {
		JsonNode value = from.get(field.getName());
		if (value != null) {
			to.set(field.getName(), value);
		}
	}
}
