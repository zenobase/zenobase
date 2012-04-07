package schema;

import java.lang.reflect.Type;

import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.node.ArrayNode;
import org.codehaus.jackson.node.ObjectNode;

import com.google.common.collect.ImmutableList;

public abstract class Field<T> {

	private final String name;
	private final Type type;
	private final String schemaType;

	protected Field(String name, Type type, String schemaType) {
		this.name = name;
		this.type = type;
		this.schemaType = schemaType;
	}

	public String getName() {
		return name;
	}

	public Type getType() {
		return type;
	}

	public ImmutableList<T> getValues(ObjectNode object) {
		ImmutableList.Builder<T> values = ImmutableList.builder();
		JsonNode node = object.get(name);
		if (node != null && node.isArray()) {
			for (JsonNode element : ((ArrayNode) node)) {
				values.add(getValue(element));
			}
		}
		else if (node != null && !node.isMissingNode() && !node.isNull()) {
			values.add(getValue(node));
		}
		return values.build();
	}

	protected abstract T getValue(JsonNode node);

	public void addValue(ObjectNode object, T value) {
		JsonNode node = object.get(name);
		if (node == null) {
			object.put(name, toJson(value));
		}
		else if (node.isArray()) {
			((ArrayNode) node).add(toJson(value));
		}
		else {
			ArrayNode arrayNode = object.putArray(name);
			arrayNode.add(node);
			arrayNode.add(toJson(value));
		}
	}

	public void addValues(ObjectNode object, Iterable<T> values) {
		JsonNode node = object.get(name);
		if (node == null) {
			ArrayNode arrayNode = object.putArray(name);
			addValues(arrayNode, values);
		}
		else if (node.isArray()) {
			addValues((ArrayNode) node, values);
		}
		else {
			ArrayNode arrayNode = object.putArray(name);
			arrayNode.add(node);
			addValues(arrayNode, values);
		}
	}

	public void setValue(ObjectNode object, T value) {
		object.put(name, toJson(value));
	}

	public void setValues(ObjectNode object, String fieldName, Iterable<T> values) {
		ArrayNode arrayNode = object.putArray(fieldName);
		addValues(arrayNode, values);
	}

	private void addValues(ArrayNode node, Iterable<T> values) {
		for (T value : values) {
			node.add(toJson(value));
		}
	}

	protected abstract JsonNode toJson(T value);

	public void configureSchema(ObjectNode schema) {
		schema.put("type", schemaType);
	}

	public void prePersist(ObjectNode object) {
		
	}

	public void postLoad(ObjectNode object) {
		
	}
}
