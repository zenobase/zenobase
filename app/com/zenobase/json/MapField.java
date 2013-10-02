package com.zenobase.json;

import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

import com.zenobase.models.Resource;

public abstract class MapField<K, V> extends Field<Map.Entry<K, V>> {

	private final Field<K> keyField;
	private final Field<V> valueField;

	public MapField(String name, Field<K> keyField, Field<V> valueField) {
		super(name, Resource.class, "nested");
		this.keyField = keyField;
		this.valueField = valueField;
	}

	@Override
	public void configureSchema(ObjectNode schema) {
		super.configureSchema(schema);
		ObjectNode properties = schema.putObject("properties");
		configureSchema(properties, keyField);
		configureSchema(properties, valueField);
	}

	@Override
	protected Map.Entry<K, V> getValue(JsonNode node) {
		return get((ObjectNode) node);
	}

	private Map.Entry<K, V> get(ObjectNode node) {
		return Maps.immutableEntry(get((ObjectNode) node, keyField), get((ObjectNode) node, valueField));
	}

	private static <T> T get(JsonNode node, Field<T> field) {
		return field.getValue((ObjectNode) node);
	}

	public static <K, V> ImmutableMap<K, V> toMap(Iterable<Map.Entry<K, V>> entries) {
		ImmutableMap.Builder<K, V> builder = ImmutableMap.builder();
		for (Map.Entry<K, V> entry : entries) {
			builder.put(entry);
		}
		return builder.build();
	}

	@Override
	public JsonNode toJson(Map.Entry<K, V> entry) {
		Preconditions.checkNotNull(entry);
		ObjectNode node = Nodes.newObject();
		keyField.setValue(node, entry.getKey());
		valueField.setValue(node, entry.getValue());
		return node;
	}
}
