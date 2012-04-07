package schema;

import java.util.Map;

import models.Resource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.Maps;
import common.Nodes;

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

	private static void configureSchema(ObjectNode properties, Field<?> field) {
		field.configureSchema(properties.putObject(field.getName()));
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

	@Override
	protected JsonNode toJson(Map.Entry<K, V> entry) {
		ObjectNode object = Nodes.newObject();
		keyField.setValue(object, entry.getKey());
		valueField.setValue(object, entry.getValue());
		return object;
	}
}
