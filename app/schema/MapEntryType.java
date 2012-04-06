package schema;

import java.util.Map;

import models.Resource;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import common.Nodes;

public abstract class MapEntryType<K, V> extends Type<Map.Entry<K, V>> {

	private final Field<K> keyField;
	private final Field<V> valueField;

	public MapEntryType(Field<K> keyField, Field<V> valueField) {
		super(Resource.class, "nested");
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
		field.getType().configureSchema(properties.putObject(field.getName()));
	}

	@Override
	protected Map.Entry<K, V> get(JsonNode node) {
		return get((ObjectNode) node);
	}

	private Map.Entry<K, V> get(ObjectNode node) {
		return Maps.immutableEntry(get((ObjectNode) node, keyField), get((ObjectNode) node, valueField));
	}

	private static <T> T get(JsonNode node, Field<T> field) {
		return Iterables.getOnlyElement(field.getType().getValues((ObjectNode) node, field.getName()));
	}

	@Override
	protected JsonNode toJson(Map.Entry<K, V> entry) {
		ObjectNode object = Nodes.newObject();
		add(object, keyField, entry.getKey());
		add(object, valueField, entry.getValue());
		return object;
	}

	private static <T> void add(ObjectNode object, Field<T> field, T value) {
		field.getType().addValue(object, field.getName(), value);
	}
}
