package com.zenobase.io;

import au.com.bytecode.opencsv.CSVWriter;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Joiner;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Sets;
import com.zenobase.json.Nodes;
import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;

public class SpreadsheetPrinter {

	private final CSVWriter writer;
	private @Nullable ImmutableList<Field> fields;

	public SpreadsheetPrinter(Writer out) {
		writer = new CSVWriter(out, ',', '"', '"', "\n");
	}

	private static class Field {

		private final ImmutableList<String> path;

		public Field(@Nullable Field parent, String field) {
			ImmutableList.Builder<String> builder = ImmutableList.builder();
			if (parent != null) {
				builder.addAll(parent.path);
			}
			this.path = builder.add(field).build();
		}

		public JsonNode get(JsonNode node) {
			for (String pathElement : path) {
				if (node.isArray()) {
					ArrayNode values = Nodes.newArray();
					for (JsonNode arrayNode : node) {
						values.add(arrayNode.path(pathElement));
					}
					node = values;
				} else {
					node = node.path(pathElement);
				}
			}
			return node;
		}

		@Override
		public boolean equals(Object that) {
			return that instanceof Field f && Objects.equals(path, f.path);
		}

		@Override
		public int hashCode() {
			return path.hashCode();
		}

		@Override
		public String toString() {
			return Joiner.on('.').join(path);
		}
	}

	private static ImmutableList<Field> getFields(ArrayNode node) {
		Set<Field> fields = Sets.newLinkedHashSet();
		for (JsonNode item : node) {
			Preconditions.checkArgument(item.isObject(), "Expected an array of objects");
			getFields((ObjectNode) item, fields, null);
		}
		return ImmutableList.copyOf(fields);
	}

	private static void getFields(ObjectNode node, Set<Field> fields, @Nullable Field parent) {
		for (Map.Entry<String, JsonNode> entry : node.properties()) {
			Field field = new Field(parent, entry.getKey());
			JsonNode value = entry.getValue();
			if (value.isArray()) {
				for (JsonNode arrayNode : value) {
					addFields(arrayNode, fields, field);
				}
			} else {
				addFields(value, fields, field);
			}
		}
	}

	private static void addFields(JsonNode node, Set<Field> fields, Field parent) {
		if (node.isValueNode()) {
			fields.add(parent);
		} else if (node.isObject()) {
			getFields((ObjectNode) node, fields, parent);
		} else {
			throw new IllegalArgumentException("Expected a value, an array of values, or an object");
		}
	}

	public void print(ArrayNode items) {
		if (fields == null) {
			fields = getFields(items);
			writer.writeNext(toString(fields));
		}
		for (JsonNode item : items) {
			Preconditions.checkArgument(item.isObject());
			writer.writeNext(toRow((ObjectNode) item));
		}
	}

	public void close() throws IOException {
		writer.close();
	}

	private static String[] toString(List<?> items) {
		String[] stringified = new String[items.size()];
		for (int i = 0; i < items.size(); ++i) {
			stringified[i] = items.get(i).toString();
		}
		return stringified;
	}

	private String[] toRow(ObjectNode node) {
		List<String> row = new ArrayList<>(Objects.requireNonNull(fields).size());
		for (Field field : fields) {
			row.add(toString(field.get(node)));
		}
		return row.toArray(new String[0]);
	}

	private static String toString(JsonNode node) {
		if (node.isValueNode() || node.isMissingNode()) {
			return node.asText();
		}
		if (node.isArray()) {
			return toString((ArrayNode) node);
		}
		throw new IllegalArgumentException("Expected a value or an array of values, but got: " + node);
	}

	private static String toString(ArrayNode node) {
		List<String> stringified = new ArrayList<>(node.size());
		for (JsonNode item : node) {
			stringified.add(toString(item));
		}
		return Joiner.on(';').join(stringified);
	}
}
